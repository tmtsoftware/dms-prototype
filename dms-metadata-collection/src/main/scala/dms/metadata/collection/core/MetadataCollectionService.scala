package dms.metadata.collection.core

import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.typed.ActorSystem
import csw.event.client.EventServiceFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.events._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS
import dms.metadata.collection.util.SubsystemExtractor

import scala.concurrent.Future

class MetadataCollectionService(
    metadataSubscriber: MetadataSubscriber,
    databaseConnector: DatabaseWriter,
    keywordValueExtractor: KeywordValueExtractor
)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val publisher    = eventService.defaultPublisher

  private val inMemoryEventServiceState = new ConcurrentHashMap[EventKey, Event]()
  private val expKey                    = StringKey.make("exposureId")

  def start(obsEventNames: Set[EventName]): Future[Done] = {
    val globalEventStreamFuture = startUpdatingInMemoryMap()
    Thread.sleep(2000) //Wait for some time so that in-memory map get some time to get eventService current state
    val observerEventStreamFuture = startCapturingSnapshots(obsEventNames)
    (globalEventStreamFuture zip observerEventStreamFuture).map(_ => Done)
  }

  // FIXME captureSnapshot is very vital, make sure it never throws exception
  def captureSnapshot(obsEvent: Event): Future[Done] = {
    // FIXME IMP: handle exceptions - 1. ClassCast, 2. NoSuchElement etc
    val exposureId: String = obsEvent.asInstanceOf[ObserveEvent](expKey).head

    println("=" * 40)
    val snapshotStart                                = System.currentTimeMillis()
    val snapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap(inMemoryEventServiceState)
    val snapshotCaptureTime                          = System.currentTimeMillis() - snapshotStart
    println("capture time " + snapshotCaptureTime)

    val snapshotWriteStart = System.currentTimeMillis()
    val writeResponse      = databaseConnector.writeSnapshot(exposureId, obsEvent.eventName.name, snapshot)

    writeResponse.foreach(_ => println("snapshot write time " + (System.currentTimeMillis() - snapshotWriteStart)))

    val extractStart = System.currentTimeMillis()
    val keywordValues: Map[String, String] =
      keywordValueExtractor.extractKeywordValuesFor(
        SubsystemExtractor.extract(exposureId),
        obsEvent,
        snapshot
      )
    println("extract time " + (System.currentTimeMillis() - extractStart))

    if (keywordValues.nonEmpty) {
      val keywordWriteStart = System.currentTimeMillis()
      val headerFuture      = databaseConnector.writeKeywordData(exposureId, keywordValues)
      headerFuture.foreach(_ => println("keyword write time " + (System.currentTimeMillis() - keywordWriteStart)))

      (writeResponse zip headerFuture).flatMap { _ =>
        val x = if (obsEvent.eventName.name == "exposureEnd") {
          publisher.publish(SystemEvent(Prefix(WFOS, "snapshot"), EventName("snapshotComplete")).madd(obsEvent.paramSet))
        } else
          Future.successful(Done)

        println("total time taken " + (System.currentTimeMillis() - snapshotStart))
        x
      }
    } else Future.successful(Done)
  }

  //FIXME handle/think about use cases :
  // When there is a slow publisher and metadata collection starts up after the slow publisher publishes, psubscribe won’t get a value for that event.
  // If metadata collection crashed and was restarted, it would not have all events if they are not changing quickly.
  // So we may need a way to get all keys once when starting
  private def startUpdatingInMemoryMap(): Future[Done] =
    metadataSubscriber
      .subscribeAll()
      // TODO try mapAsync and run as it is concurrent hashmap
      .runForeach { event => inMemoryEventServiceState.put(event.eventKey, event) }

  private def startCapturingSnapshots(obsEventNames: Set[EventName]): Future[Done] =
    metadataSubscriber
      .subscribeObsEvents(obsEventNames)
      .mapAsync(10)(captureSnapshot)
      .run()

}

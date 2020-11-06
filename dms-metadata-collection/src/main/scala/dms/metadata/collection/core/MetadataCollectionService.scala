package dms.metadata.collection.core

import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.typed.ActorSystem
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{Event, EventKey, EventName, ObserveEvent}
import dms.metadata.collection.config.KeywordConfigReader
import dms.metadata.collection.util.SubsystemExtractor

import scala.concurrent.Future

class MetadataCollectionService(
    metadataSubscriber: MetadataSubscriber,
    databaseConnector: DatabaseWriter,
    headerConfigReader: KeywordConfigReader
)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext

  val inMemoryEventServiceState = new ConcurrentHashMap[EventKey, Event]()
  val expKey: Key[String]       = StringKey.make("exposureId")

  def start(obsEventNames: Set[EventName]): Future[Done] = {
    val globalEventStreamFuture = startUpdatingInMemoryMap()
    Thread.sleep(2000) //Wait for some time so that in-memory map get some time to get eventService current state
    val observerEventStreamFuture = startCapturingSnapshots(obsEventNames)
    (globalEventStreamFuture zip observerEventStreamFuture).map(_ => Done)
  }

  def captureSnapshot(obsEvent: Event): Future[Done] = {
    val exposureId: String = obsEvent.asInstanceOf[ObserveEvent](expKey).head

    val snapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap(inMemoryEventServiceState)

    val snapshotFuture = databaseConnector.writeSnapshot(exposureId, obsEvent.eventName.name, snapshot)

    try {
      val keywordValues: Map[String, String] =
        headerConfigReader.extractKeywordValuesFor(
          SubsystemExtractor.extract(exposureId),
          obsEvent,
          snapshot
        )

      val headerFuture = databaseConnector.writeKeywordData(exposureId, keywordValues)
      (snapshotFuture zip headerFuture).map { _ => Done }
    }
    catch {
      case exception: Exception => throw exception
    }

  }

  private def startUpdatingInMemoryMap(): Future[Done] = {
    metadataSubscriber
      .subscribeAll()
      .runForeach { event =>
        inMemoryEventServiceState.put(event.eventKey, event)
      } // TODO try mapAsync and run as it is concurrent hashmap
  }

  private def startCapturingSnapshots(obsEventNames: Set[EventName]): Future[Done] = {
    metadataSubscriber
      .subscribeObsEvents(obsEventNames)
      .mapAsync(10)(captureSnapshot)
      .run()
  }

}

package dms.metadata.collection.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import csw.event.api.scaladsl.EventSubscription
import csw.event.client.EventServiceFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.events._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS
import dms.metadata.collection.util.SubsystemExtractor
import org.HdrHistogram.Histogram

import scala.concurrent.Future

class MetadataCollectionService(
    metadataSubscriber: MetadataSubscriber,
    databaseConnector: DatabaseWriter,
    keywordValueExtractor: KeywordValueExtractor
)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext
  val capturedSnapshots            = new AtomicInteger(0)
  val warmupSnapshots              = 15
  val totalSnapshotsToCapture: Int = warmupSnapshots + 300

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val publisher    = eventService.defaultPublisher

  private val inMemoryEventServiceState = new ConcurrentHashMap[EventKey, Event]()
  private val expKey                    = StringKey.make("exposureId")

  val snapshotCaptureHist = new Histogram(3)
  val snapshotPersistHist = new Histogram(3)
  val keywordExtractHist  = new Histogram(3)
  val keywordPersistHist  = new Histogram(3)

  printResultsTask(snapshotCaptureHist, "SNAPSHOT CAPTURE")
  printResultsTask(snapshotPersistHist, "SNAPSHOT PERSIST")
  printResultsTask(keywordExtractHist, "KEYWORD EXTRACT")
  printResultsTask(keywordPersistHist, "KEYWORD PERSIST")

  def start(obsEventNames: Set[EventName]): Future[Done] = {
    val globalsubscriptionResult = startUpdatingInMemoryMap()
    Thread.sleep(2000) //Wait for some time so that in-memory map get some time to get eventService current state
    val observerEventSubResult = startCapturingSnapshots(obsEventNames)

    observerEventSubResult._2.flatMap(_ => globalsubscriptionResult._1.unsubscribe())
  }

  // FIXME captureSnapshot is very vital, make sure it never throws exception
  def captureSnapshot(obsEvent: Event): Future[Done] = {
    // FIXME IMP: handle exceptions - 1. ClassCast, 2. NoSuchElement etc
    val exposureId: String = obsEvent.asInstanceOf[ObserveEvent](expKey).head

    println("=" * 40)
    val snapshotStart                                = System.currentTimeMillis()
    val snapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap(inMemoryEventServiceState)
    val snapshotCaptureTime                          = System.currentTimeMillis() - snapshotStart
    recordValue(snapshotCaptureHist, snapshotCaptureTime)
    println("capture time " + snapshotCaptureTime)

    val snapshotWriteStart = System.currentTimeMillis()
    println("number of eventKeys = " + snapshot.size())
    val writeResponse = databaseConnector.writeSnapshot(exposureId, obsEvent.eventName.name, snapshot)

    writeResponse.foreach(_ => {
      val snapshotPersistTime = System.currentTimeMillis() - snapshotWriteStart
      recordValue(snapshotPersistHist, snapshotPersistTime)
      println("snapshot write time " + snapshotPersistTime)
    })

    val extractStart = System.currentTimeMillis()
    val keywordValues: Map[String, String] =
      keywordValueExtractor.extractKeywordValuesFor(
        SubsystemExtractor.extract(exposureId),
        obsEvent,
        snapshot
      )
    val keywordExtractTime = System.currentTimeMillis() - extractStart
    recordValue(keywordExtractHist, keywordExtractTime)
    println("extract time " + keywordExtractTime)

    if (keywordValues.nonEmpty) {
      val keywordWriteStart = System.currentTimeMillis()
      val headerFuture      = databaseConnector.writeKeywordData(exposureId, keywordValues)
      headerFuture.foreach(_ => {
        val keywordPersistTime = System.currentTimeMillis() - keywordWriteStart
        recordValue(keywordPersistHist, keywordPersistTime)
        println("keyword write time " + keywordPersistTime)
      })

      (writeResponse zip headerFuture).flatMap { _ =>
        val x = if (obsEvent.eventName.name == "exposureEnd") {
          publisher.publish(SystemEvent(Prefix(WFOS, "snapshot"), EventName("snapshotComplete")).madd(obsEvent.paramSet))
        } else
          Future.successful(Done)
        capturedSnapshots.incrementAndGet()
        println("total time taken " + (System.currentTimeMillis() - snapshotStart))
        x
      }
    } else Future.successful(Done)
  }

  //FIXME handle/think about use cases :
  // When there is a slow publisher and metadata collection starts up after the slow publisher publishes, psubscribe won’t get a value for that event.
  // If metadata collection crashed and was restarted, it would not have all events if they are not changing quickly.
  // So we may need a way to get all keys once when starting
  private def startUpdatingInMemoryMap(): (EventSubscription, Future[Done]) = {
    val (mat, source) = metadataSubscriber
      .subscribeAll()
      .preMaterialize()

    // TODO try mapAsync and run as it is concurrent hashmap
    val eventualDone = source.runForeach { event => inMemoryEventServiceState.put(event.eventKey, event) }
    (mat, eventualDone)
  }

  private def startCapturingSnapshots(obsEventNames: Set[EventName]): (EventSubscription, Future[Done]) = {
    val (mat, source) = metadataSubscriber
      .subscribeObsEvents(obsEventNames)
      .take(totalSnapshotsToCapture)
      .mapAsync(10)(captureSnapshot)
      .preMaterialize()
    (mat, source.run())
  }

  // to remove warm up data
  private def recordValue(histogram: Histogram, value: Long): Unit =
    if (capturedSnapshots.get() > warmupSnapshots) histogram.recordValue(value)

  private def printResultsTask(histogram: Histogram, resultsFor: String): Unit = {
    CoordinatedShutdown(actorSystem).addTask(
      CoordinatedShutdown.PhaseBeforeActorSystemTerminate,
      "plot results for " + resultsFor
    ) { () =>
      println("=" * 80)
      println("Metadata collection perf results for : " + resultsFor)
      println("50 percentile : " + histogram.getValueAtPercentile(50).toDouble)
      println("90 percentile : " + histogram.getValueAtPercentile(90).toDouble)
      println("99 percentile : " + histogram.getValueAtPercentile(99).toDouble)
      println("=" * 80)
      Future.successful(Done)
    }
  }
}

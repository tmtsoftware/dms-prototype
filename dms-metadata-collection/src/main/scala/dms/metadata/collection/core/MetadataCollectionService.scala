package dms.metadata.collection.core

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentHashMap, Executors}

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import csw.event.api.scaladsl.EventSubscription
import csw.event.client.EventServiceFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.events._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS
import dms.metadata.collection.util.SubsystemExtractor
import org.HdrHistogram.Histogram

import scala.concurrent.{ExecutionContext, Future}

class MetadataCollectionService(
    metadataSubscriber: MetadataSubscriber,
    databaseConnector: DatabaseWriter,
    keywordValueExtractor: KeywordValueExtractor
)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext
  val capturedSnapshots            = new AtomicInteger(0)
  val warmupSnapshots              = 30
  val totalSnapshotsToCapture: Int = 300000

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val publisher    = eventService.defaultPublisher

  private val inMemoryEventServiceState = new ConcurrentHashMap[EventKey, Event]()
  private val expKey                    = StringKey.make("exposureId")

  val snapshotCaptureHist = new Histogram(3)
  val snapshotPersistHist = new Histogram(3)
  val keywordExtractHist  = new Histogram(3)
  val keywordPersistHist  = new Histogram(3)
  val totalTimeHist       = new Histogram(3)

  printResultsTask(snapshotCaptureHist, "SNAPSHOT CAPTURE")
  printResultsTask(snapshotPersistHist, "SNAPSHOT PERSIST")
  printResultsTask(keywordExtractHist, "KEYWORD EXTRACT")
  printResultsTask(keywordPersistHist, "KEYWORD PERSIST")
  printResultsTask(totalTimeHist, "TOTAL TIME")

  def start(obsEventNames: Set[EventName]): Future[Done] = {
    val globalSubscriptionResult = startUpdatingInMemoryMap()
    Thread.sleep(2000) //Wait for some time so that in-memory map get some time to get eventService current state
    val observerEventSubResult = startCapturingSnapshots(obsEventNames)

    observerEventSubResult._2.flatMap(_ => globalSubscriptionResult._1.unsubscribe())
  }

  // FIXME captureSnapshot is very vital, make sure it never throws exception
  def captureSnapshot(obsEvent: Event): Future[(Event, ConcurrentHashMap[EventKey, Event], Long)] =
    Future {
      val snapshotStart = System.currentTimeMillis()
      val tuple: (Event, ConcurrentHashMap[EventKey, Event], Long) =
        (obsEvent, new ConcurrentHashMap(inMemoryEventServiceState), snapshotStart)
      val captureTime = System.currentTimeMillis() - snapshotStart
      println("=" * 40)
      println("capture time = " + captureTime)
      recordValue(snapshotCaptureHist, captureTime)
      tuple
    }(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))

  // FIXME captureSnapshot is very vital, make sure it never throws exception
  def saveSnapshot(obsEvent: Event, snapshot: ConcurrentHashMap[EventKey, Event], snapshotStart: Long): Future[Done] = {
    // FIXME IMP: handle exceptions - 1. ClassCast, 2. NoSuchElement etc
    val exposureId: String = obsEvent.asInstanceOf[ObserveEvent](expKey).head
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
        if (obsEvent.eventName.name == "exposureEnd") {
          publisher.publish(SystemEvent(Prefix(WFOS, "snapshot"), EventName("snapshotComplete")).madd(obsEvent.paramSet))
        }
        capturedSnapshots.incrementAndGet()
        val endTime = System.currentTimeMillis() - snapshotStart
        recordValue(totalTimeHist, endTime)
        println("total time taken " + endTime)
        Future.successful(Done)
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
      .drop(warmupSnapshots)
      .buffer(100, OverflowStrategy.dropHead)
      .mapAsync(1)(captureSnapshot)
      .mapAsyncUnordered(4) {
        case (obsEvent, snapshot, snapshotStartTime) => saveSnapshot(obsEvent, snapshot, snapshotStartTime)
      }
      .preMaterialize()
    (mat, source.run())
  }

  // to remove warm up data
  private def recordValue(histogram: Histogram, value: Long): Unit =
    histogram.recordValue(value)

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

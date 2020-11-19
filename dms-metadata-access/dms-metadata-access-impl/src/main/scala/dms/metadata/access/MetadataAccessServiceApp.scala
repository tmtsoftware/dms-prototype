package dms.metadata.access

import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.event.client.EventServiceFactory
import csw.location.client.ActorSystemFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import org.HdrHistogram.Histogram
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object MetadataAccessServiceApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  private val dslContext: DSLContext = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)

  private val headerProcessor                              = new HeaderProcessor
  private val databaseConnector                            = new DatabaseReader(dslContext)
  private val metadataAccessService: MetadataAccessService = new MetadataAccessImpl(databaseConnector, headerProcessor)

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val subscriber   = eventService.defaultSubscriber

  val accessedCount = new AtomicInteger(0)
  val warmupCount   = 5                                       // warmupSnapshots of  collection service / 3
  val totalCount    = warmupCount + 1 /*invalid event*/ + 100 //  10 mins

  // to remove warm up data
  private def recordValue(histogram: Histogram, value: Long): Unit =
    if (accessedCount.longValue() > warmupCount) histogram.recordValue(value)

  val accessTimeHist = new Histogram(3)

  val expIdKey = StringKey.make("exposureId")

  println("time taken , count of keywords")
  private val metadataAccessServiceDoneF: Future[Done] = subscriber
    .subscribe(Set(EventKey(Prefix(WFOS, "snapshot"), EventName("snapshotComplete"))))
    .take(totalCount)
    .runForeach { e =>
      val exposureId: String = e.asInstanceOf[SystemEvent](expIdKey).head

      val startTime = System.currentTimeMillis()
      metadataAccessService
        .getFITSHeader(exposureId)
        .foreach(x => {
          val accessTime = System.currentTimeMillis() - startTime
          recordValue(accessTimeHist, accessTime)
          accessedCount.incrementAndGet()
          println(s"$accessTime , ${x.lines().count()}")
        })
    }

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "plot  results") { () =>
    println("=" * 80)
    println("ACCESS SERVICE perf results")
    println("50 percentile : " + accessTimeHist.getValueAtPercentile(50).toDouble)
    println("90 percentile : " + accessTimeHist.getValueAtPercentile(90).toDouble)
    println("99 percentile : " + accessTimeHist.getValueAtPercentile(99).toDouble)
    println("=" * 80)
    Future.successful(Done)
  }

  metadataAccessServiceDoneF.map { _ =>
    system.terminate()
  }
}

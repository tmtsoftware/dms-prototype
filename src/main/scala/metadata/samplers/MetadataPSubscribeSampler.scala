package metadata.samplers

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}
import metadata.subscriber.RedisGlobalSubscriber
import metadata.util.{DbUtil, SamplerUtil}
import org.jooq.DSLContext

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MetadataPSubscribeSampler(
    globalSubscriber: RedisGlobalSubscriber,
    eventService: EventService,
    dbUtil: DbUtil
)(implicit actorSystem: ActorSystem[_]) {

  private val samplerUtil     = new SamplerUtil
  private val samplerUtilSlow = new SamplerUtil
  private val eventSubscriber = eventService.defaultSubscriber

  var currentState = new ConcurrentHashMap[EventKey, Event]() // mutable map with variable ref

  var numberOfSnapshots                       = 0d
  val eventTimeDiffList: ListBuffer[Long]     = scala.collection.mutable.ListBuffer()
  val slowEventTimeDiffList: ListBuffer[Long] = scala.collection.mutable.ListBuffer()
  var outlierNegEventCount                    = 0
  var outlierEventCount                       = 0
  var outlierNegSlowEventCount                = 0
  var outlierSlowEventCount                   = 0
  val snapshotTimeList: ListBuffer[Long]      = scala.collection.mutable.ListBuffer()

  def snapshot(obsEvent: Event): Unit = {
    val startTime = System.currentTimeMillis()

    val lastSnapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap(currentState)

    val exposureId =
      obsEvent.paramSet.find(p => p.keyName.equals("exposureId")).map(e => e.items.head match { case s: String => s }).get
    val storeInDb = dbUtil.store(exposureId, obsEvent.eventName.name, lastSnapshot)
    Await.result(storeInDb, 10.seconds)
    val endTime = System.currentTimeMillis()

    val event             = lastSnapshot.getOrDefault(EventKey("ESW.filter.wheel"), Event.badEvent())
    val slowEvent         = lastSnapshot.getOrDefault(EventKey("ESW.filter.wheel1"), Event.badEvent())
    val eventTimeDiff     = Duration.between(event.eventTime.value, obsEvent.eventTime.value).toMillis
    val slowEventTimeDiff = Duration.between(slowEvent.eventTime.value, obsEvent.eventTime.value).toMillis
    val snapshotTime      = endTime - startTime

    //Outliers
    if (eventTimeDiff < 0) outlierNegEventCount += 1
    if (eventTimeDiff > 10) outlierEventCount += 1
    if (slowEventTimeDiff < 0) outlierNegSlowEventCount += 1
    if (slowEventTimeDiff > 1000) outlierSlowEventCount += 1

    //Event time diff
    eventTimeDiffList.addOne(eventTimeDiff)
    slowEventTimeDiffList.addOne(slowEventTimeDiff)

    //Snapshot time diff
    snapshotTimeList.addOne(snapshotTime)
    samplerUtil.recordHistogram(Math.abs(eventTimeDiff), Math.abs(snapshotTime))
    samplerUtilSlow.recordHistogram(Math.abs(eventTimeDiff), Math.abs(snapshotTime))

    println(s"${lastSnapshot.size} ,$eventTimeDiff ,$slowEventTimeDiff,$snapshotTime")
  }

  def subscribeObserveEvents(): Future[Done] =
    eventSubscriber
      .subscribe(Set(EventKey("esw.observe.exposureStart")))
      .drop(5)
      .take(1000)
      .runForeach(snapshot)

  def start(): Future[Done] = {
    //print aggregates
    CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "print aggregates") { () =>
      samplerUtil.printAggregates(eventTimeDiffList, snapshotTimeList)
      println(s"outlierEventCount = $outlierEventCount")
      println(s"outlierNegEventCount = $outlierNegEventCount")
      println(s"outlierSlowEventCount = $outlierSlowEventCount")
      println(s"outlierNegSlowEventCount = $outlierNegSlowEventCount")
      Future.successful(Done)
    }

    val subscribeFuture = subscribeObserveEvents() // fire in background

    globalSubscriber
      .subscribeAll()
      .runForeach { e => currentState.put(e.eventKey, e) }

    subscribeFuture
  }
}

object MetadataPSubscribeSampler extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val redisClient: RedisClient                    = RedisClient.create()

  private val host = "localhost"
  private val port = 26379
  private val eventualRedisURI: Future[RedisURI] =
    Future.successful(RedisURI.Builder.sentinel(host, port, "eventServer").build())

  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val dslContext: DSLContext =
    Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "mydb"), 10.seconds)

  private val util = new DbUtil(dslContext)(system.executionContext)

  private val eventService                            = new EventServiceFactory(RedisStore(redisClient)).make(host, port)
  private val globalSubscriber: RedisGlobalSubscriber = RedisGlobalSubscriber.make(redisClient, eventualRedisURI)

  private val sampler = new MetadataPSubscribeSampler(globalSubscriber, eventService, util)

  println("numberOfKeys", "eventTimeDiff", "slowEventTimeDiff", "snapshotTime")

  Await.result(sampler.start(), 20.minutes)
  system.terminate()
  redisClient.shutdown()
}

package metadata.minimal

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
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{Event, EventKey, ObserveEvent}
import io.lettuce.core.{RedisClient, RedisURI}
import metadata.subscriber.RedisGlobalSubscriber
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MinimalPSubscribeSampler(
    globalSubscriber: RedisGlobalSubscriber,
    eventService: EventService,
    dbUtil: DbUtil
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  val eventSubscriber = eventService.defaultSubscriber
  var currentState    = new ConcurrentHashMap[EventKey, Event]() // mutable map with variable ref
  val expKey          = StringKey.make("exposureId")

  def snapshot(obsEvent: Event): Future[Done] = {
    val startTime = System.currentTimeMillis()

    val lastSnapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap(currentState)
    val exposureId                                       = obsEvent.asInstanceOf[ObserveEvent](expKey).head

    val storeInDb = dbUtil.store(exposureId, obsEvent.eventName.name, lastSnapshot)
    storeInDb.onComplete { _ =>
      val endTime      = System.currentTimeMillis()
      val snapshotTime = endTime - startTime

      println(s"${lastSnapshot.size} ,$snapshotTime")

    }
    storeInDb
  }

  def subscribeObserveEvents(): Future[Done] =
    eventSubscriber
      .subscribe(Set(EventKey("esw.observe.exposureStart"), EventKey("esw.observe.exposureEnd")))
      .drop(5)
      .take(1000)
      .mapAsync(8)(snapshot)
      .run()

  def start(): Future[Done] = {
    CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "print aggregates") { () =>
      Future.successful(Done)
    }

    val subscribeFuture = subscribeObserveEvents() // fire in background

    globalSubscriber
      .subscribeAll()
      .runForeach { e => currentState.put(e.eventKey, e) }

    subscribeFuture
  }
}

object MinimalPSubscribeSampler extends App {
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
  Await.result(util.cleanTable(), 10.seconds)

  private val eventService                            = new EventServiceFactory(RedisStore(redisClient)).make(host, port)
  private val globalSubscriber: RedisGlobalSubscriber = RedisGlobalSubscriber.make(redisClient, eventualRedisURI)

  private val sampler = new MinimalPSubscribeSampler(globalSubscriber, eventService, util)

  println("numberOfKeys", "snapshotTime")

  Await.result(sampler.start(), 20.minutes)
  system.terminate()
  redisClient.shutdown()
}

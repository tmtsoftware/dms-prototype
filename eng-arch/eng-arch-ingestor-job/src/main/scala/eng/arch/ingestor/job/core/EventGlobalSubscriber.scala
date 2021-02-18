package eng.arch.ingestor.job.core

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.params.events.Event
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RomaineFactory
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}

import scala.concurrent.Future

class EventGlobalSubscriber(redisURI: RedisURI, redisClient: RedisClient)(implicit actorSystem: ActorSystem[_]) {
  import eng.arch.ingestor.job.util.RomaineCodecs._
  import actorSystem.executionContext

  private lazy val romaineFactory   = new RomaineFactory(redisClient)
  private val globalSubscriptionKey = "*.*.*"

  def subscribeAll(): Source[Event, RedisSubscription] = subscribe(globalSubscriptionKey)

  private def subscribe(patterns: String*): Source[Event, RedisSubscription] = {
    val redisSubscriptionApi: RedisSubscriptionApi[String, Event] =
      romaineFactory.redisSubscriptionApi(Future.successful(redisURI))

    redisSubscriptionApi
      .psubscribe(patterns.toList, OverflowStrategy.LATEST) // todo: think about overflow
      .map(_.value)
  }
}

package dms.metadata.collection.core

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.EventSubscription
import csw.params.events.{Event, EventName}
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RomaineFactory
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}

import scala.concurrent.Future

class MetadataSubscriber(redisURI: RedisURI, redisClient: RedisClient)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  import dms.metadata.collection.util.RomaineCodecs._

  private lazy val romaineFactory   = new RomaineFactory(redisClient)
  private val globalSubscriptionKey = "*.*.*"

  def subscribeAll(): Source[Event, EventSubscription] = subscribe(globalSubscriptionKey)
  def subscribeObsEvents(eventNames: Set[EventName]): Source[Event, EventSubscription] =
    subscribe(eventNames.map(n => s"*.*.${n.name}").toList: _*)

  private def subscribe(patterns: String*): Source[Event, EventSubscription] = {
    val redisSubscriptionApi: RedisSubscriptionApi[String, Event] =
      romaineFactory.redisSubscriptionApi(Future.successful(redisURI))

    redisSubscriptionApi
      .psubscribe(patterns.toList, OverflowStrategy.LATEST) // todo: think about overflow
      .map(_.value)
      .mapMaterializedValue(subscription)
  }

  // FIXME: Can we use RedisSubscription and not do this?
  private def subscription(rs: RedisSubscription) =
    new EventSubscription {
      override def unsubscribe(): Future[Done] = rs.unsubscribe()
      override def ready(): Future[Done]       = rs.ready()
    }
}

object MetadataSubscriber {
  def make(
      redisClient: RedisClient,
      redisURI: RedisURI
  )(implicit system: ActorSystem[_]): MetadataSubscriber = new MetadataSubscriber(redisURI, redisClient)
}

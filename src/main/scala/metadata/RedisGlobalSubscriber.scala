package metadata

import java.nio.ByteBuffer

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.EventSubscription
import csw.params.events.{Event, EventKey}
import io.bullet.borer.Cbor.DecodingConfig
import io.bullet.borer.{Cbor, Input, Output}
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RomaineFactory
import romaine.codec.RomaineCodec
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}

import scala.concurrent.Future
import scala.util.control.NonFatal

object EventConverter {
  import csw.params.core.formats.ParamCodecs._

  def toEvent[Chunk: Input.Provider](bytes: Chunk): Event = {
    try {
      Cbor.decode(bytes).withConfig(DecodingConfig(readDoubleAlsoAsFloat = true)).to[Event].value
    }
    catch {
      case NonFatal(_) => Event.badEvent()
    }
  }

  def toBytes[Chunk: Output.ToTypeProvider](event: Event): Chunk = {
    Cbor.encode(event).to[Chunk].result
  }
}

object RomaineCodecs {

  implicit val eventKeyRomaineCodec: RomaineCodec[EventKey] =
    RomaineCodec.stringCodec.bimap(_.key, EventKey.apply)

  implicit val eventRomaineCodec: RomaineCodec[Event] =
    RomaineCodec.byteBufferCodec.bimap[Event](EventConverter.toBytes[ByteBuffer], EventConverter.toEvent)
}

class RedisGlobalSubscriber(redisURI: Future[RedisURI], redisClient: RedisClient)(implicit actorSystem: ActorSystem[_]) {
  import RomaineCodecs._
  import actorSystem.executionContext

  private lazy val romaineFactory   = new RomaineFactory(redisClient)
  private val globalSubscriptionKey = "*.*.*"

  def subscribeAll(): Source[Event, EventSubscription] = {
    val redisSubscriptionApi: RedisSubscriptionApi[String, Event] = romaineFactory.redisSubscriptionApi(redisURI)

    redisSubscriptionApi
      .psubscribe(List(globalSubscriptionKey), OverflowStrategy.LATEST) // todo: think about overflow
      .map(_.value)
      .mapMaterializedValue(subscription)
  }

  private def subscription(rs: RedisSubscription) =
    new EventSubscription {
      override def unsubscribe(): Future[Done] = rs.unsubscribe()
      override def ready(): Future[Done]       = rs.ready()
    }
}

object RedisGlobalSubscriber {
  def make(
      redisClient: RedisClient,
      redisURI: Future[RedisURI]
  )(implicit system: ActorSystem[_]): RedisGlobalSubscriber = new RedisGlobalSubscriber(redisURI, redisClient)
}

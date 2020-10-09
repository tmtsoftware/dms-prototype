package metadata

import akka.actor.typed.ActorSystem
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi

import scala.concurrent.Future

class RedisApi(redisURI: Future[RedisURI], redisClient: RedisClient)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext
  import RomaineCodecs._
  private lazy val romaineFactory = new RomaineFactory(redisClient)

  private val redisApi: RedisAsyncApi[String, Event] = romaineFactory.redisAsyncApi(redisURI)

  def keys(pattern: String): Future[List[EventKey]] = redisApi.keys(pattern).map(_.map(EventKey.apply))

}

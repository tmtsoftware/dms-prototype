package dms.metadata.collection

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.location.client.ActorSystemFactory
import csw.params.events.EventName
import dms.metadata.collection.config.KeywordConfigReader
import dms.metadata.collection.core.{DatabaseWriter, MetadataSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Main extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext

  private val eventServicePort         = 26379
  private val host                     = "localhost"
  private val redisClient: RedisClient = RedisClient.create()
  private val eventualRedisURI: Future[RedisURI] =
    Future.successful(RedisURI.Builder.sentinel(host, eventServicePort, "eventServer").build())
  private val dslContext: DSLContext                 = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)
  private val eventService: EventService             = new EventServiceFactory(RedisStore(redisClient)).make(host, eventServicePort)
  private val metadataSubscriber: MetadataSubscriber = MetadataSubscriber.make(redisClient, eventualRedisURI)

  private val databaseConnector         = new DatabaseWriter(dslContext)
  private val headerConfigReader        = new KeywordConfigReader
  private val metadataCollectionService = new MetadataCollectionService(metadataSubscriber, databaseConnector, headerConfigReader)

  val obsEventNames: Set[EventName] =
    Set("exposureStart", "exposureMiddle", "exposureEnd").map(EventName) //FIXME Read from Config
  private val metadataCollectionServiceFuture: Future[Done] = metadataCollectionService.start(obsEventNames)
  metadataCollectionServiceFuture.map { _ =>
    system.terminate()
    redisClient.shutdown()
  }

}

package dms.metadata.collection

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.client.ActorSystemFactory
import csw.params.events.EventName
import dms.metadata.collection.config.{KeywordConfigReader, ObserveEventNameConfigReader}
import dms.metadata.collection.core.{DatabaseWriter, MetadataCollectionService, MetadataSubscriber}
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
  private val metadataSubscriber: MetadataSubscriber = MetadataSubscriber.make(redisClient, eventualRedisURI)

  private val databaseConnector             = new DatabaseWriter(dslContext)
  private val headerConfigReader            = new KeywordConfigReader
  private val metadataCollectionService     = new MetadataCollectionService(metadataSubscriber, databaseConnector, headerConfigReader)
  private val obsEventNames: Set[EventName] = ObserveEventNameConfigReader.read()

  private val metadataCollectionServiceFuture: Future[Done] = metadataCollectionService.start(obsEventNames)
  metadataCollectionServiceFuture.map { _ =>
    system.terminate()
    redisClient.shutdown()
  }

}

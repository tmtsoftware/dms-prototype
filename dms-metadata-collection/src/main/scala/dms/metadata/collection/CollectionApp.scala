package dms.metadata.collection

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.client.ActorSystemFactory
import dms.metadata.collection.config.{KeywordConfigReader, ObserveEventNameConfigReader}
import dms.metadata.collection.core.{DatabaseWriter, KeywordValueExtractor, MetadataCollectionService, MetadataSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object CollectionApp {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private val eventServicePort                       = 26379
  private val host                                   = "localhost"
  private val redisClient: RedisClient               = RedisClient.create()
  private val redisURI: RedisURI                     = RedisURI.Builder.sentinel(host, eventServicePort, "eventServer").build()
  private val dslContext: DSLContext                 = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)
  private val metadataSubscriber: MetadataSubscriber = MetadataSubscriber.make(redisClient, redisURI)

  private val databaseConnector                = new DatabaseWriter(dslContext)
  private val keywordConfigReader              = new KeywordConfigReader
  private val valueExtractor                   = new KeywordValueExtractor(keywordConfigReader.headerConfigs)
  private val metadataCollectionService        = new MetadataCollectionService(metadataSubscriber, databaseConnector, valueExtractor)
  private val obsEventNames                    = ObserveEventNameConfigReader.read()
  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(system)

  def main(args: Array[String]): Unit = {
    metadataCollectionService.start(obsEventNames)

    coordinatedShutdown
      .addTask(PhaseBeforeServiceUnbind, "unregister-collection-service") { () =>
        redisClient.shutdown()
        system.terminate()
        println("stopping collection service")
        Future.successful(Done)
      }
  }
}

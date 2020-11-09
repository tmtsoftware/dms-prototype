package dms.metadata.access

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.client.ActorSystemFactory
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val dslContext: DSLContext                      = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)

  private val headerProcessor                              = new HeaderProcessor
  private val databaseConnector                            = new DatabaseReader(dslContext)
  private val metadataAccessService: MetadataAccessService = new MetadataAccessImpl(databaseConnector, headerProcessor)

  //TODO start http server
  private val header: String = Await.result(metadataAccessService.getFITSHeader("2034A-P054-O010-IRIS-BLU1-SCI1-1"), 5.seconds)
  println(header)

  system.terminate()
}

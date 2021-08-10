package dms.metadata.access

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import csw.database.DatabaseServiceFactory
import csw.location.client.ActorSystemFactory
import csw.network.utils.SocketUtils
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import org.jooq.DSLContext

import java.nio.file.Path
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AccessWiring(port: Option[Int], externalConfigPath: Option[Path]) extends PredefinedFromStringUnmarshallers {

  implicit lazy val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "metadata-access-server")

  lazy private val dslContext: DSLContext                       = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)
  lazy private val headerProcessor                              = new HeaderProcessor(externalConfigPath)
  lazy private val databaseConnector                            = new DatabaseReader(dslContext)
  lazy private val metadataAccessService: MetadataAccessService = new MetadataAccessImpl(databaseConnector, headerProcessor)
  lazy val coordinatedShutdown: CoordinatedShutdown             = CoordinatedShutdown(system)
  lazy val serverPort: Int                                      = port.getOrElse(SocketUtils.getFreePort)

  def start(): Unit = {
    import system.executionContext

    lazy val route =
      path("fits-header") {
        get {
          parameter("exp-id", "keywords".as(CsvSeq[String]).optional) { (expId, keywords) =>
            keywords match {
              case Some(value) => complete(metadataAccessService.getFITSHeader(expId, value.toList))
              case None        => complete(metadataAccessService.getFITSHeader(expId))
            }
          }
        }
      }

    lazy val httpServerBinding =
      Await.result(Http().newServerAt("localhost", serverPort).bind(route), 5.seconds)

    println(
      s"Access Service is running on http://${httpServerBinding.localAddress.getHostString}:$serverPort/fits-header?exp-id=some-exposure-id"
    )

    coordinatedShutdown
      .addTask(PhaseBeforeServiceUnbind, "unregister-access-service") { () =>
        httpServerBinding
          .unbind()
          .onComplete(_ => system.terminate())
        println("stopping access service")
        Future.successful(Done)
      }
  }
}

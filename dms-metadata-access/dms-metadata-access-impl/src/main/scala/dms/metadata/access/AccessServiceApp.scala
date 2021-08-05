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
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object AccessServiceApp extends PredefinedFromStringUnmarshallers {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "metadata-access-server")
  private val dslContext: DSLContext                      = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)

  private val headerProcessor                              = new HeaderProcessor
  private val databaseConnector                            = new DatabaseReader(dslContext)
  private val metadataAccessService: MetadataAccessService = new MetadataAccessImpl(databaseConnector, headerProcessor)
  val coordinatedShutdown: CoordinatedShutdown             = CoordinatedShutdown(system)

  def main(args: Array[String]): Unit = {

    implicit val executionContext = system.executionContext

    val route =
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

    val port              = args(0).toInt
    val httpServerBinding = Await.result(Http().newServerAt("localhost", port).bind(route), 5.seconds)

    println(
      s"Access Service is running on http://${httpServerBinding.localAddress.getHostString}:$port/fits-header?exp-id=some-exposure-id"
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

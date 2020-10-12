package metadata

import java.sql.{Connection, DatabaseMetaData}

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object TestApp extends App {

  implicit val value: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private val client: LocationService = HttpLocationServiceFactory.makeLocalClient

  private val eventualContext: Future[DSLContext] =
    new DatabaseServiceFactory(value).makeDsl(client, "postgres", "postgres", "postgres")

  private val context: DSLContext = Await.result(eventualContext, 10.seconds)

  private val connection: Connection = context.diagnosticsConnection()

  private val data: DatabaseMetaData = connection.getMetaData

  print(data)
}

package metadata.db

import java.sql.Timestamp

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import metadata.util.{DbUtil, SourceUtil}
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object QueryApp extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val dslContext: DSLContext =
    Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "mydb"), 10.seconds)

  private val util = new DbUtil(dslContext)(system.executionContext)
  Await.result(util.cleanTable(), 10.seconds)

  var counter: Iterator[Int] = (50 to 200).iterator

  def queryMetadata() = {
    val startTime = System.currentTimeMillis()
    val getDatabaseQuery =
      dslContext.resultQuery(
        s"select * from event_snapshots where exposure_id='2034A-P054-O010-WFOS-BLU1-SCI1-${counter.next()}'"
      )

    val resultSet = getDatabaseQuery.fetchAsyncScala[(String, String, String, String, String, Timestamp, String)]

    resultSet.map { _ => println(System.currentTimeMillis() - startTime) }

  }

  SourceUtil
    .tick(0.seconds, 2.seconds)
    .runForeach(_ => queryMetadata())
}

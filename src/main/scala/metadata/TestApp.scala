package metadata

import java.sql.Timestamp

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.database.scaladsl.JooqExtentions.{RichQuery, RichResultQuery}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import org.jooq.DSLContext
import org.jooq.impl.DSL.jsonbObject

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object TestApp extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  import system.executionContext

  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private val context: DSLContext = Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "mydb"), 10.seconds)

  private val event: SystemEvent = SystemEvent(Prefix(ESW, "filter"), EventName("wheel"))

  println(event.eventTime)

//  private val nObject = new JSONObject(new util.HashMap(1))

//  private val records = context.resultQuery("select count(*) from event_snapshots;").fetchAsyncScala[Int]
//  println(Await.result(records, 10.seconds) + "=========================")

  private val eventualInteger = context
    .query(
      "INSERT INTO event_snapshots values (?,?,?,?,?,?,?)",
      "expId1",
      "obs_event_name1",
      "event_source1",
      "event_name",
      "event_id",
      new Timestamp(0),
      jsonbObject()
    )
    .executeAsyncScala()

  println(Await.result(eventualInteger, 10.seconds))
  println("-------------------------------")

  system.terminate()
  Await.result(system.whenTerminated, 10.seconds)
}

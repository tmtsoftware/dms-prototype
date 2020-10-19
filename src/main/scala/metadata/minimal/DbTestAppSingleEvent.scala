package metadata.minimal

import java.sql.Timestamp

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.database.scaladsl.JooqExtentions.RichQuery
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.formats.ParamCodecs.paramEncExistential
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{Event, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import io.bullet.borer.Json
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object DbTestAppSingleEvent extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val locationService: LocationService            = HttpLocationServiceFactory.makeLocalClient
  private val context: DSLContext                         = Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "mydb"), 10.seconds)

  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
    val payload = StringKey.make("payloadKey").set("0" * size)
    event.add(payload)
  }

  val `12_MB_EVENT` = addPayload(SystemEvent(Prefix(ESW, "filter"), EventName("wheel5")), 1024 * 1024 * 12)

  (1 to 100).foreach { _ =>
    val startTime = System.currentTimeMillis()
    val eventualInteger = context
      .query(
        s"""
        INSERT INTO event_snapshots values ${eventSeq("expId7000", "startExposure", `12_MB_EVENT`)}
      """
      )
      .executeAsyncScala()
    Await.result(eventualInteger, 5.minutes)
    println(System.currentTimeMillis() - startTime)
  }

  system.terminate()
  Await.result(system.whenTerminated, 10.seconds)

  private def eventSeq(expId: String, obsEventName: String, event: Event) =
    s"""('$expId','$obsEventName','${event.source.toString()}','${event.eventName.name}',
        '${event.eventId.id}','${Timestamp.from(event.eventTime.value)}',
        '${Json.encode(event.paramSet).toUtf8String}'::Json)
    """
}

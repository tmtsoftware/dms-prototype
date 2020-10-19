package metadata.minimal

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object DbTestAppFlattenedEvents extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  private val locationService: LocationService          = HttpLocationServiceFactory.makeLocalClient
  private val context: DSLContext                       = Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "mydb"), 10.seconds)
  private val event: SystemEvent                        = addPayload(SystemEvent(Prefix(ESW, "filter"), EventName("wheel5")), 1024 * 5)
  private val mymap: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap[EventKey, Event]()
  (1 to 2300).foreach { i =>
    val key = EventKey(s"${event.eventKey.key}_$i")
    mymap.put(key, event)
  }
  val dbutil = new DbUtil(context)
  (1 to 1000).foreach { i =>
    val startTime    = System.currentTimeMillis()
    val eventualDone = dbutil.store(s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", "exposureEnd", mymap)
    Await.result(eventualDone, 5.minutes)
    println(mymap.size(), System.currentTimeMillis() - startTime)

  }
  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
    val payload = StringKey.make("payloadKey").set("0" * size)
    event.add(payload)
  }
}

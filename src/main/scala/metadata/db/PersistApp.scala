package metadata.db

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, ComponentType, TcpRegistration}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.generics.Parameter
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{CSW, ESW}
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object PersistApp extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val locationService: LocationService            = HttpLocationServiceFactory.makeLocalClient
  private val dbConnection: TcpConnection                 = TcpConnection(ComponentId(Prefix(CSW, "DatabaseServer"), ComponentType.Service))
  Await.result(locationService.register(TcpRegistration(dbConnection, 5432)), 15.seconds)
  private val context: DSLContext =
    Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "postgres"), 10.seconds)
  private val event: SystemEvent = addPayload(SystemEvent(Prefix(ESW, "filter"), EventName("wheel5")), 5120)

  val dbutil = new DbUtil(context)

  //CLEAN TABLE
  Await.result(dbutil.cleanTable(), 5.seconds)

  val exposures = List("exposureStart", "exposureMiddle", "exposureEnd")

  (1 to 20).foreach { i =>
    exposures.map { obseventname =>
      val startTime = System.currentTimeMillis()
      val snapshot  = createSnapshot(s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", obseventname)
      //      dbutil.batch(snapshot)
      //      val eventualDone = dbutil.store(snapshot)
      Await.result(dbutil.batchVersion2(snapshot), 5.seconds)
      //      Await.result(eventualDone, 1.seconds)
      println(
        s"items: ${snapshot.length}, time : ${System.currentTimeMillis() - startTime} millis >>>>>>>>>>>writing>>>>>>>>>>>>>"
      )
    }

  }

  //  (1 to 10).foreach { _ =>
  //    QueryApp.queryMetadata()
  //  }

  def createSnapshot(expId: String, obsEventName: String): Seq[EventRecord] = {
    (1 to 2300).map { i =>
      EventRecord.create(
        expId,
        obsEventName,
        SystemEvent(event.source, EventName(s"${event.eventName.name}_$i"), event.paramSet)
      )
    }
  }

  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
    val payload2: Set[Parameter[_]] = ParamSetData.paramSet
    //    val payload = StringKey.make("payloadKey").set("0" * size)
    event.madd(payload2)
  }
}

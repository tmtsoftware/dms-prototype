package metadata.db

import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.generics.Parameter
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import io.bullet.borer.Json
import metadata.util.SourceUtil
import org.jooq.{DSLContext, JSON}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object QueryApp extends App {
  import csw.params.core.formats.ParamCodecs.paramDecExistential

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val dslContext: DSLContext =
    Await.result(new DatabaseServiceFactory(system).makeDsl(locationService, "mydb"), 10.seconds)

//  private val util = new DbUtil(dslContext)(system.executionContext)
//  Await.result(util.cleanTable(), 10.seconds)

  var counter: Iterator[Int] = (1 to 1000).iterator

  def queryMetadata() = {
    val startTime = System.currentTimeMillis()
    val getDatabaseQuery =
      dslContext.resultQuery(
        s"select * from event_snapshots where exposure_id='2034A-P054-O010-WFOS-BLU1-SCI1-${counter.next()}' and obs_event_name='exposureEnd'"
      )

    val eventualSnapshot = getDatabaseQuery.fetchAsyncScala[(String, String, String, String, String, Timestamp, JSON)]

    val snapshotFromDb                                  = Await.result(eventualSnapshot, 1.minutes)
    val snapshotMap: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap[EventKey, Event]()
//    println(snapshotFromDb.size)
    snapshotFromDb.map { event =>
      val (_, _, source, eventName, _, eventTime, paramSetJSON) =
        event                      // expId,obsEventName,source,eventName,eventID,eventTime,paramJSON
      UTCTime(eventTime.toInstant) // FixMe ???

      val paramSet    = Json.decode(paramSetJSON.data().getBytes(StandardCharsets.UTF_8)).to[Set[Parameter[_]]].value
      val systemEvent = SystemEvent(Prefix(source), EventName(eventName), paramSet)
      snapshotMap.put(EventKey(Prefix(event._3), EventName(event._4)), systemEvent)
    }
    println(System.currentTimeMillis() - startTime)

  }

  SourceUtil
    .tick(0.seconds, 1.seconds)
    .runForeach(_ => queryMetadata())
}

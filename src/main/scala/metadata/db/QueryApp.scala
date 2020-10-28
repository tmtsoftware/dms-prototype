package metadata.db

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.generics.Parameter
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import io.bullet.borer.Json
import metadata.util.{DbUtil, SourceUtil}
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object QueryApp extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  implicit val context: DSLContext             = DbSetup.dslContext
  private val dbUtil                           = new DbUtil(context)
  private val snapshotTable                    = "event_snapshots"

  var counter: Iterator[Int] = (1 to 1000).iterator

  SourceUtil
    .tick(0.seconds, 1.seconds)
    .runForeach(_ => QueryAppUtil.queryMetadata(snapshotTable, context, counter.next()))
}

object QueryAppUtil {
  import csw.params.core.formats.ParamCodecs.paramDecExistential

  def queryMetadata(tableName: String, dslContext: DSLContext, counter: Int)(implicit executionContext: ExecutionContext) = {
    val startTime = System.currentTimeMillis()
    val getDatabaseQuery =
      dslContext.resultQuery(
        s"select * from $tableName where exposure_id='2034A-P054-O010-WFOS-BLU1-SCI1-$counter'"
      )

    val eventualSnapshot = getDatabaseQuery.fetchAsyncScala[(String, String, String, String, String, Timestamp, Array[Byte])]

    val snapshotFromDb                                  = Await.result(eventualSnapshot, 1.minutes)
    val snapshotMap: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap[EventKey, Event]()
    //    println(snapshotFromDb.size)
    snapshotFromDb.map { event =>
      val (_, _, source, eventName, _, eventTime, param) =
        event                      // expId,obsEventName,source,eventName,eventID,eventTime,paramJSON
      UTCTime(eventTime.toInstant) // FixMe ???

      val paramSet    = Json.decode(param).to[Set[Parameter[_]]].value
      val systemEvent = SystemEvent(Prefix(source), EventName(eventName), paramSet)
      snapshotMap.put(EventKey(Prefix(event._3), EventName(event._4)), systemEvent)
    }
    println(System.currentTimeMillis() - startTime)

  }

}

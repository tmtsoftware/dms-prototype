package metadata.util

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.ActorSystem
import csw.database.scaladsl.JooqExtentions.RichQuery
import csw.params.events.{Event, EventKey}
import io.bullet.borer.Json
import org.jooq.DSLContext

import scala.concurrent.Future.unit
import scala.concurrent.{Future, blocking}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps

class DbUtil(dslContext: DSLContext)(implicit system: ActorSystem[_]) {
  import csw.params.core.formats.ParamCodecs.paramEncExistential

  def cleanTable(): Future[Integer] = dslContext.query("delete from event_snapshots").executeAsyncScala()

  def batchInsertSnapshots(
      expId: String,
      obsEventName: String,
      snapshot: ConcurrentHashMap[EventKey, Event],
      table: String
  ): Future[Array[Int]] = {
    val query = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?,?,?,?,?)")
    snapshot.values().asScala.foreach { event =>
      query.bind(
        expId,
        obsEventName,
        event.source.toString(),
        event.eventName.name,
        event.eventId.id,
        Timestamp.from(event.eventTime.value),
        Json.encode(event.paramSet).toUtf8String
      )
    }
    blocking { query.executeAsync().asScala }
  }

  def batchInsertHeaderData(table: String, expId: String, headersValueMap: Map[String, String]): Future[AnyRef] = {
    val query = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?)")
    headersValueMap.foreach { headerEntry =>
      query.bind(
        expId,
        headerEntry._1,
        headerEntry._2
      )
    }
    if (headersValueMap.nonEmpty) query.executeAsync().asScala
    else Future.successful(unit) ///FIXME edge case
  }

}

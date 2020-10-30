package metadata.util

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.database.scaladsl.JooqExtentions.RichQuery
import csw.params.events.{Event, EventKey}
import io.bullet.borer.Json
import metadata.db.EventRecord
import org.jooq.DSLContext

import scala.concurrent.{Future, blocking}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps

class DbUtil(dslContext: DSLContext)(implicit system: ActorSystem[_]) {
  import csw.params.core.formats.ParamCodecs.paramEncExistential
  import system.executionContext

  def cleanTable(): Future[Integer] = dslContext.query("delete from event_snapshots").executeAsyncScala()

  def store(expId: String, obsEventName: String, snapshot: ConcurrentHashMap[EventKey, Event]): Future[Done] = {
    dslContext
      .query(s"INSERT INTO event_snapshots values ${snapshotSql(expId, obsEventName, snapshot)}")
      .executeAsyncScala()
      .map(_ => Done)
  }

  def batchInsertSingle(table: String, snapshot: Seq[EventRecord]): Array[Int] = {
    var batch = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?,?,?,?,?)")
    snapshot.foreach { eventRecord =>
      batch = batch.bind(
        eventRecord.expId,
        eventRecord.obsEventName,
        eventRecord.source,
        eventRecord.eventName,
        eventRecord.eventId,
        eventRecord.eventTime,
        eventRecord.paramSet
      )
    }
    batch.execute()
  }

  def batchInsertParallelSnapshots(table: String, snapshot: Seq[EventRecord]): Future[Done] = {
    Source(snapshot).grouped(500).mapAsyncUnordered(5)(batchInsertSnapshots(table, _)).run()
  }

  private def batchInsertSnapshots(table: String, batch: Seq[EventRecord]) = {
    val query = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?,?,?,?,?)")
    batch.foreach { eventRecord =>
      query.bind(
        eventRecord.expId,
        eventRecord.obsEventName,
        eventRecord.source,
        eventRecord.eventName,
        eventRecord.eventId,
        eventRecord.eventTime,
        Json.encode(eventRecord.paramSet).toByteArray
      )
    }
    blocking { query.executeAsync().asScala }
  }

  def batchInsertHeaderData(table: String, expId: String, obsEventName: String, headersValueMap: List[(String, String)]) = {
    val query = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?,?)")
    headersValueMap.foreach { headerEntry =>
      query.bind(
        expId,
        obsEventName,
        headerEntry._1,
        headerEntry._2
      )
    }
    query.executeAsync().asScala
  }

  def write(eventRecord: EventRecord): Future[Int] = {
    Future {
      blocking {
        dslContext
          .query(
            "INSERT INTO event_snapshots VALUES (?,?,?,?,?,?,?)",
            eventRecord.expId,
            eventRecord.obsEventName,
            eventRecord.source,
            eventRecord.eventName,
            eventRecord.eventId,
            eventRecord.eventTime,
            eventRecord.paramSet
          )
          .execute()
      }
    }
  }

  private def eventSql(expId: String, obsEventName: String, event: Event) =
    s"""('$expId','$obsEventName','${event.source.toString()}','${event.eventName.name}',
        '${event.eventId.id}','${Timestamp.from(event.eventTime.value)}',
        '${Json.encode(event.paramSet).toUtf8String}'::Json)
    """

  private def snapshotSql(expId: String, obsEventName: String, snapshot: ConcurrentHashMap[EventKey, Event]): String =
    snapshot.values().asScala.toList.map(eventSql(expId, obsEventName, _)).mkString(",")

}

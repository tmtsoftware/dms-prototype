package metadata.util

import java.sql.Timestamp

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.database.scaladsl.JooqExtentions.RichQuery
import csw.params.events.Event
import io.bullet.borer.Json
import org.jooq.DSLContext

import scala.concurrent.Future.unit
import scala.concurrent.{Future, blocking}
import scala.jdk.FutureConverters.CompletionStageOps

class DbUtil(dslContext: DSLContext)(implicit system: ActorSystem[_]) {
  import csw.params.core.formats.ParamCodecs.paramEncExistential

  def cleanTable(): Future[Integer] = dslContext.query("delete from event_snapshots").executeAsyncScala()

  def batchInsertParallelSnapshots(expId: String, obsEventName: String, snapshot: Seq[Event], table: String): Future[Done] = {
    Source(snapshot).grouped(500).mapAsyncUnordered(5)(batchInsertSnapshots(expId, obsEventName, _, table)).run()
  }

  private def batchInsertSnapshots(expId: String, obsEventName: String, batch: Seq[Event], table: String): Future[Array[Int]] = {
    val query = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?,?,?,?,?)")
    batch.foreach { event =>
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

  def batchInsertHeaderData(
      table: String,
      expId: String,
      obsEventName: String,
      headersValueMap: List[(String, Option[String])]
  ): Future[AnyRef] = {
    val query = dslContext.batch(s"INSERT INTO $table VALUES (?,?,?,?)")
    headersValueMap.foreach { headerEntry =>
      query.bind(
        expId,
        obsEventName,
        headerEntry._1,
        headerEntry._2.getOrElse("not found")
      )
    }
    if (headersValueMap.nonEmpty) query.executeAsync().asScala
    else Future.successful(unit)
  }

}

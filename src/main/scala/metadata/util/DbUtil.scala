package metadata.util

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import csw.database.scaladsl.JooqExtentions.RichQuery
import csw.params.events.{Event, EventKey}
import io.bullet.borer.Json
import org.jooq.DSLContext

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class DbUtil(dslContext: DSLContext)(implicit executionContext: ExecutionContext) {
  import csw.params.core.formats.ParamCodecs.paramEncExistential

  private def eventSql(expId: String, obsEventName: String, event: Event) =
    s"""('$expId','$obsEventName','${event.source.toString()}','${event.eventName.name}',
        '${event.eventId.id}','${Timestamp.from(event.eventTime.value)}',
        '${Json.encode(event.paramSet).toUtf8String}'::Json)
    """

  private def snapshotSql(expId: String, obsEventName: String, snapshot: ConcurrentHashMap[EventKey, Event]): String =
    snapshot.values().asScala.toList.map(eventSql(expId, obsEventName, _)).mkString(",")

  def store(expId: String, obsEventName: String, snapshot: ConcurrentHashMap[EventKey, Event]): Future[Done] = {
    dslContext
      .query(s"INSERT INTO event_snapshots values ${snapshotSql(expId, obsEventName, snapshot)}")
      .executeAsyncScala()
      .map(_ => Done)
  }
}

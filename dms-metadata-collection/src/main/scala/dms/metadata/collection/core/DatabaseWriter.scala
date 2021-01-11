package dms.metadata.collection.core

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.actor.typed.ActorSystem
import csw.params.events.{Event, EventKey}
import io.bullet.borer.Json
import org.jooq.DSLContext

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps

class DatabaseWriter(dslContext: DSLContext)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  import csw.params.core.formats.ParamCodecs.paramEncExistential

  // FIXME domain model for data? List(Header(k, v))?
  def writeKeywordData(expId: String, data: Map[String, String]): Future[Done] = {

    // FIXME this should be take care at caller side
    require(data.nonEmpty) // FIXME what to do if empty keywordData received to persist - FAIL/IGNORE ?

    val query = dslContext.batch(s"INSERT INTO keyword_values VALUES (?,?,?)")
    data.map {
      case (header, value) => query.bind(expId, header, value)
    }

    query.executeAsync().asScala.map(_ => Done)
  }

  def writeSnapshot(expId: String, obsEventName: String, snapshot: ConcurrentHashMap[EventKey, Event]): Future[Done] = {

    require(!snapshot.isEmpty) // FIXME what to do if empty snapshot received to persist - FAIL/IGNORE ?

    val query = dslContext.batch(s"INSERT INTO event_snapshots VALUES (?,?,?,?,?,?,?)")
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
    query.executeAsync().asScala.map(_ => Done)
  }

}

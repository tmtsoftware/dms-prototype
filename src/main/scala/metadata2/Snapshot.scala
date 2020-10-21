package metadata2

import java.time.{Instant, LocalDateTime, ZoneId}

import csw.params.core.formats.ParamCodecs._
import csw.params.core.generics.Parameter
import csw.params.events.Event
import io.bullet.borer.{Cbor, Json}

object Snapshot {

  def createRow(expId: String, obsEventName: String, event: Event): SnapshotRow =
    SnapshotRow(
      expId,
      obsEventName,
      event.source.toString,
      event.eventName.name,
      event.eventId.id,
      event.eventTime.value.atZone(ZoneId.systemDefault()).toLocalDateTime,
      Json.encode(event.paramSet).toUtf8String
    )

  def create(expId: String, obsEventName: String, events: List[Event]): List[SnapshotRow] =
    events.map(createRow(expId, obsEventName, _))

  // paramSet = byte[]
  def createRow2(expId: String, obsEventName: String, event: Event): SnapshotRow2 =
    SnapshotRow2(
      expId,
      obsEventName,
      event.source.toString,
      event.eventName.name,
      event.eventId.id,
      event.eventTime.value.atZone(ZoneId.systemDefault()).toLocalDateTime,
      Cbor.encode(event.paramSet).toByteArray
    )

  def create2(expId: String, obsEventName: String, events: List[Event]): List[SnapshotRow2] =
    events.map(createRow2(expId, obsEventName, _))
}

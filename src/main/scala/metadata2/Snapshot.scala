package metadata2

import java.time.ZoneId

import csw.params.core.formats.ParamCodecs._
import csw.params.events.Event
import io.bullet.borer.Json

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
}

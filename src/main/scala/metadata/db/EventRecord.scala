package metadata.db

import java.sql.Timestamp

import csw.params.core.formats.ParamCodecs.paramEncExistential
import csw.params.events.Event
import io.bullet.borer.Json

case class EventRecord(
    expId: String,
    obsEventName: String,
    source: String,
    eventName: String,
    eventId: String,
    eventTime: Timestamp,
    paramSet: Array[Byte]
)

case class EventRecord2(
    exposureid: String,
    obseventname: String,
    source: String,
    eventname: String,
    eventid: String,
    eventtime: Timestamp,
    paramset: Array[Byte]
)

object EventRecord {
  def create(expId: String, obsEventName: String, event: Event): EventRecord = {
    EventRecord(
      expId = expId,
      obsEventName = obsEventName,
      source = event.source.toString(),
      eventName = event.eventName.name,
      eventId = event.eventId.id,
      eventTime = Timestamp.from(event.eventTime.value),
      paramSet = Json.encode(event.paramSet).toByteArray
    )
  }
}

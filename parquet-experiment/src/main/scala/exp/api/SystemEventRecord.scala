package exp.api

import java.time.{LocalDateTime, ZoneOffset}

import csw.params.core.formats.ParamCodecs._
import csw.params.events.SystemEvent
import io.bullet.borer.Json

case class SystemEventRecord(
    hour: String,
    minute: String,
    source: String,
    eventId: String,
    eventName: String,
    eventTime: String,
    seconds: Long,
    nanos: Long,
    paramSet: String
)

object SystemEventRecord {
  def generate(systemEvent: SystemEvent): SystemEventRecord = {
    SystemEventRecord(
      LocalDateTime.ofInstant(systemEvent.eventTime.value, ZoneOffset.UTC).getHour.toString,
      LocalDateTime.ofInstant(systemEvent.eventTime.value, ZoneOffset.UTC).getMinute.toString,
      systemEvent.source.toString(),
      systemEvent.eventId.id,
      systemEvent.eventName.name,
      systemEvent.eventTime.value.toString,
      systemEvent.eventTime.value.getEpochSecond,
      systemEvent.eventTime.value.getNano,
      Json.encode(systemEvent.paramSet).toUtf8String
    )
  }
}

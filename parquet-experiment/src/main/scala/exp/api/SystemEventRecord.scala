package exp.api

import java.time.{LocalDateTime, ZoneOffset}

import csw.params.core.formats.ParamCodecs._
import csw.params.events.SystemEvent
import io.bullet.borer.Json

case class SystemEventRecord(
    date: String,
    hour: String,
    minute: String,
    source: String,
    eventId: String,
    eventName: String,
    eventTime: String,
    paramSet: String
)

object SystemEventRecord {
  def generate(systemEvent: SystemEvent): SystemEventRecord = {
    val time = LocalDateTime.ofInstant(systemEvent.eventTime.value, ZoneOffset.UTC)
    SystemEventRecord(
      time.toLocalDate.toString,
      time.getHour.toString,
      time.getMinute.toString,
      systemEvent.source.toString(),
      systemEvent.eventId.id,
      systemEvent.eventName.name,
      systemEvent.eventTime.value.toString,
      Json.encode(systemEvent.paramSet).toUtf8String
    )
  }
}

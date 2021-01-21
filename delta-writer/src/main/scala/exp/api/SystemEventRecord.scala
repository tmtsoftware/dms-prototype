package exp.api

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import exp.data.ParamSetJson

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
  def generate(): SystemEventRecord = generate(UUID.randomUUID().toString)

  def generate(eventId: String): SystemEventRecord = {
    val instant = Instant.now()
    val time    = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    SystemEventRecord(
      time.toLocalDate.toString,
      time.getHour.toString,
      time.getMinute.toString,
      "wfos.blue.filter",
      eventId,
      "filter wheel",
      instant.toString,
      ParamSetJson.jsonString
    )
  }
}

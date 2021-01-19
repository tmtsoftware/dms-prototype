package exp.api

import java.time.Instant
import java.util.UUID

import exp.data.ParamSetJson

case class SystemEventRecord(
    source: String,
    eventId: String,
    eventName: String,
    eventTime: String,
    seconds: Long,
    nanos: Long,
    paramSet: String,
    hour: String,
    minute: String
)

object SystemEventRecord {
  def generate(): SystemEventRecord = generate(0, "startEvent", UUID.randomUUID().toString)

  def generate(exposureId: Long, obsEventName: String, eventId: String): SystemEventRecord = {
    val instant = Instant.now()
    SystemEventRecord(
      "wfos.blue.filter",
      eventId,
      "filter wheel",
      instant.toString,
      instant.getEpochSecond,
      instant.getNano,
      ParamSetJson.jsonString,
      obsEventName,
      exposureId.toString
    )
  }
}

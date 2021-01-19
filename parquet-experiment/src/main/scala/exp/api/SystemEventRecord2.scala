package exp.api

import csw.params.events.SystemEvent
import io.bullet.borer.{Cbor, Json}
import csw.params.core.formats.ParamCodecs._

case class SystemEventRecord2(
    exposureId: String,
    obsEventName: String,
    eventId: String,
    source: String,
    eventName: String,
    eventTime: String,
    seconds: Long,
    nanos: Long,
    paramSet: Array[Byte]
)

object SystemEventRecord2 {
  def generate(exposureId: Long, obsEventName: String, systemEvent: SystemEvent): SystemEventRecord2 = {
    SystemEventRecord2(
      exposureId.toString,
      obsEventName,
      systemEvent.eventId.id,
      systemEvent.source.toString(),
      systemEvent.eventName.name,
      systemEvent.eventTime.value.toString,
      systemEvent.eventTime.value.getEpochSecond,
      systemEvent.eventTime.value.getNano,
      Json.encode(systemEvent.paramSet).toByteArray
    )
  }
}

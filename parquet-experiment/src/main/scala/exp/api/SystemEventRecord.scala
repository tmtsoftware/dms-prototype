package exp.api

import csw.params.core.formats.ParamCodecs._
import csw.params.events.Event
import io.bullet.borer.derivation.MapBasedCodecs
import io.bullet.borer.{Codec, Json}

import java.time.{LocalDateTime, ZoneOffset}

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
  def generate(systemEvent: Event): SystemEventRecord = {
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

  implicit lazy val seCodec: Codec[SystemEventRecord] = MapBasedCodecs.deriveCodec

}

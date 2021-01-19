package csw

import java.time.Instant

import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import exp.api.SystemEventRecord
import io.bullet.borer.Json
import csw.params.core.formats.ParamCodecs._
import csw.params.core.generics.KeyType.StringKey
import csw.prefix.models.Subsystem.ESW

object EventFactory {
  private val prefix    = Prefix("wfos.blue.filter")
  private val eventName = EventName("filter wheel")

  def generateEvent(): SystemEvent = SystemEvent(prefix, eventName, ParamSetData.paramSet)

  def generateEvent(id: Int): SystemEvent = SystemEvent(Id(id.toString), prefix, eventName, UTCTime.now(), ParamSetData.paramSet)

  def fromRecord(record: SystemEventRecord): SystemEvent = {
    import record._
    SystemEvent(
      Id(eventId),
      Prefix(source),
      EventName(record.eventName),
      UTCTime(Instant.ofEpochSecond(seconds, nanos)),
      Json.decode(paramSet.getBytes()).to[Set[Parameter[_]]].value
    )
  }

  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
    val payload = StringKey.make("payloadKey").set("0" * size)
    event.add(payload)
  }

  def generateTestEvent(): SystemEvent = {
    addPayload(SystemEvent(Prefix(ESW, "filter"), EventName("wheel5")), 1024 * 5)
  }

}

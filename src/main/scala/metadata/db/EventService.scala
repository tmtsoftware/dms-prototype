package metadata.db

import java.sql.Timestamp

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.events.{Event, EventKey, EventName, SystemEvent}

case class EventRecord(
    expId: String,
    obsEventName: String,
    source: String,
    eventName: String,
    eventId: String,
    eventTime: Timestamp,
    paramSet: Set[Parameter[_]]
)

object EventService {
  private def createRecord(expId: String, obsEventName: String, event: Event): EventRecord = {
    EventRecord(
      expId = expId,
      obsEventName = obsEventName,
      source = event.source.toString(),
      eventName = event.eventName.name,
      eventId = event.eventId.id,
      eventTime = Timestamp.from(event.eventTime.value),
      paramSet = event.paramSet
    )
  }

  def createSnapshot(expId: String, obsEventName: String, event: SystemEvent) = {
    (1 to 2300).map { i =>
      val systemEvent = addPayload(SystemEvent(event.source, EventName(s"event_key_$i"), event.paramSet), i)
      val eventRecord = EventService.createRecord(
        expId,
        obsEventName,
        systemEvent
      )
      EventKey(event.source, EventName(eventRecord.eventName)) -> eventRecord
    }.toMap
  }

  def addPayload(event: SystemEvent, i: Int): SystemEvent = {
    val payload2: Set[Parameter[_]] = ParamSetData.paramSet
    event
      .madd(payload2)
      .madd(StringKey.make(s"param_key_$i").set(s"param-value-$i"))

  }
}

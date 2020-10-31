package metadata.db

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

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
  def createSnapshot(event: SystemEvent): ConcurrentHashMap[EventKey, Event] = {
    val javaMap = new ConcurrentHashMap[EventKey, Event]()
    (1 to 2300).map { i =>
      val systemEvent: Event = addPayload(SystemEvent(event.source, EventName(s"event_key_$i"), event.paramSet), i)
      javaMap.put(EventKey(systemEvent.source, systemEvent.eventName), systemEvent)
    }
    javaMap
  }

  def addPayload(event: SystemEvent, i: Int): SystemEvent = {
    val payload2: Set[Parameter[_]] = ParamSetData.paramSet
    event
      .madd(payload2)
      .madd(StringKey.make(s"param_key_$i").set(s"param-value-$i"))
      .madd(StringKey.make(s"param_key_$i$i").set(s"param-value-$i$i"))

  }
}

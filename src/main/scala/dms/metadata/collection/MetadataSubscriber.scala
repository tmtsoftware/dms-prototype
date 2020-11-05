package dms.metadata.collection

import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.{EventService, EventSubscription}
import csw.params.events.{Event, EventKey, EventName}
import csw.prefix.models.Prefix

class MetadataSubscriber(
    globalSubscriber: RedisGlobalSubscriber,
    eventService: EventService
) {

  def subscribeAll(): Source[Event, EventSubscription] = {
    globalSubscriber
      .subscribeAll()
  }

  def subscribeObserveEvents(obsEventNames: Set[EventName]): Source[Event, EventSubscription] = {
    val value = obsEventNames.map(e => EventKey(Prefix("esw.observe"), e))
    eventService.defaultSubscriber
      .subscribe(
        Set(EventKey("esw.observe.exposureStart"))
      ) // FIXME Add pattern support in API and pass obsEventNames => (*.*.obsEventName)
  }
}

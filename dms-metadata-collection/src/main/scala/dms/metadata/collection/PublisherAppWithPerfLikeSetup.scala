package dms.metadata.collection

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.client.EventServiceFactory
import csw.location.client.ActorSystemFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object PublisherAppWithPerfLikeSetup extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val publisher    = eventService.defaultPublisher

  def publishEvent(noOfEvents: Int, every: FiniteDuration, eventName: String) = {
    val publisher = eventService.makeNewPublisher()
    (1 to noOfEvents).map { i =>
      val event = SystemEvent(Prefix(ESW, "filter"), EventName(s"${eventName}_$i"))
      publisher.publish(Some(addPayload(event, i)), every)
    }
  }

  def addPayload(event: SystemEvent, i: Int): SystemEvent = {
    val payload2: Set[Parameter[_]] = ParamSetData.paramSet
    event
      .madd(payload2)
      .madd(StringKey.make(s"param_key_$i").set(s"param-value-$i"))
      .madd(StringKey.make(s"param_key_$i$i").set(s"param-value-$i$i"))
  }

  def publishObsEvent(exposureId: String, every: FiniteDuration) = {

    def cycle[T](elems: T*): LazyList[T] = LazyList(elems: _*) #::: cycle(elems: _*)

    val iterator   = cycle("exposureStart", "exposureMid", "exposureEnd").iterator
    def obsEvent() = ObserveEvent(Prefix(ESW, "observe"), EventName(iterator.next()))

    val expIdKey     = StringKey.make("exposureId")
    val expIdCounter = LazyList.from(0).iterator

    def eventGenerator() = {
      val counter = expIdCounter.next() / 3
      val event   = obsEvent()
      println(s"publishing observer event : ${event.eventName} $counter")
      Some(event.add(expIdKey.set(s"$exposureId-$counter")))
    }

    publisher.publish(eventGenerator(), every)
  }

  //  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
  //    val payload = StringKey.make("payloadKey").set("0" * size)
  //    event.add(payload)
  //  }

  // ============== TEST BEGINS ============

  // ========= Publish ObserveEvent 1 msg/sec =============
  publishObsEvent("2034A-P054-O010-WFOS-BLU1-SCI1", 2.second)

  // ========= Publish Fast Event 1 msg/10ms =============
//  publishEvent(1, 10.millis, "1_10_ms", 5120)

  // ======================================================
  // Publish 2300 unique event keys =>
  // 500 keys = 1msg/sec
  // 500 keys = 1msg/500ms
  // 500 keys = 1msg/200ms
  // 500 keys = 1msg/100ms
  // 300 keys = 1msg/50ms
  // ======================================================
  publishEvent(500, 1.second, "500_1_sec")
  publishEvent(500, 500.millis, "500_500_ms")
  publishEvent(500, 200.millis, "500_200_ms")
  publishEvent(500, 100.millis, "500_100_ms")
  publishEvent(300, 50.millis, "event_key")
}

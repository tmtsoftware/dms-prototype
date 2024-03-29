package simulator

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.event.client.EventServiceFactory
import csw.location.client.ActorSystemFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.ExposureId
import csw.params.events.{EventName, IRDetectorEvent, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object PublisherAppWithPerfLikeSetup extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val publisher    = eventService.defaultPublisher

  var count = new AtomicInteger()
  def publishEvent(noOfEvents: Int, every: FiniteDuration, eventName: String, eventSize: Int) = {
    val publisher = eventService.makeNewPublisher()
    (1 to noOfEvents).map { i =>
      val event = SystemEvent(Prefix(ESW, "filter"), EventName(s"event_key_$i"))
      publisher.publish(
        {
          if (count.incrementAndGet() >= 15000) {
            System.exit(0)
          }
          Some(addPayload(event, i))
        },
        every
      )
    }
  }

  def addPayload(event: SystemEvent, i: Int): SystemEvent = {
    val payload2: Set[Parameter[_]] = ParamSetData.paramSet
    event
//      .madd(payload2)
      .madd(StringKey.make(s"param_key_$i").set(s"param-value-$i"))
      .madd(StringKey.make(s"param_key_$i$i").set(s"param-value-$i$i"))

  }

  def publishObsEvent(name: String, exposureId: String, every: FiniteDuration) = {
    var counter = 1

    def eventGenerator() = {
      println("publishing observer event : " + counter)
      val observeEvent =
        IRDetectorEvent.exposureStart(
          Prefix(ESW, "observe"),
          ExposureId(s"2020A-001-123-IRIS-IMG-SCI0-${"%04d".format(counter)}")
        )
      counter += 1
      Some(observeEvent)
    }

    publisher.publish(eventGenerator(), every)
  }

  //  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
  //    val payload = StringKey.make("payloadKey").set("0" * size)
  //    event.add(payload)
  //  }

  // ============== TEST BEGINS ============

  // ========= Publish ObserveEvent 1 msg/sec =============
  publishObsEvent("ObserveEvent.ExposureStart", "2034A-P054-O010-WFOS-BLU1-SCI1", 1.second)

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
  publishEvent(500, 1.second, "500_1_sec", 5120)    //500
  publishEvent(500, 500.millis, "500_500_ms", 5120) //1000
//  publishEvent(500, 200.millis, "500_200_ms", 5120) //2500
//  publishEvent(500, 100.millis, "500_100_ms", 5120) //5000
//  publishEvent(300, 50.millis, "300_50_ms", 5120)   //6000
}

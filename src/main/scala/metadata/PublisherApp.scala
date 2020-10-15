package metadata

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import csw.event.client.EventServiceFactory
import csw.location.client.ActorSystemFactory
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Units.NoUnits
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object PublisherApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  private val eventService = new EventServiceFactory().make("localhost", 26379)

  private val publisher = eventService.defaultPublisher
  private var counter   = 1
  // observe event
  private val observeEventSource: Future[Done] = Source
    .repeat("abc")
    .throttle(1, 1.seconds)
    .take(1200)
    .runForeach(_ => {
      val exposureId = s"2034A-P054-O010-WFOS-BLU1-SCI1-$counter"
      val param      = Parameter("exposureId", KeyType.StringKey, scala.collection.mutable.ArraySeq(exposureId), NoUnits)
      println(s"Publishing observe event $counter")
      counter += 1
      publisher.publish(ObserveEvent(Prefix(ESW, "observe"), EventName("expstr"), Set(param)))
    })

  // 100Hz event
  Source
    .tick(0.seconds, 10.millis, ()) //100 Hz * how many? 10?
    .runForeach(_ => publisher.publish(SystemEvent(Prefix(ESW, "filter"), EventName("wheel"))))

  Await.result(observeEventSource, 18.minutes)

  system.terminate()

}

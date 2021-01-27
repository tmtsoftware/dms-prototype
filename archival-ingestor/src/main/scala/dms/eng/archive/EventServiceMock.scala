package dms.eng.archive

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object EventServiceMock {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(Behaviors.empty, "bla")

  var count = new AtomicInteger()
  def publish(streamId: Int, noOfEvents: Int, every: FiniteDuration): Source[SystemEvent, Future[Done]] = {
    Source.queue[SystemEvent](1024, OverflowStrategy.dropHead).mapMaterializedValue { q =>
      stream(streamId).throttle(noOfEvents, every).runForeach(q.offer)
    }
  }

  def stream(streamId: Int) =
    Source
      .fromIterator(() => Iterator.from(1))
      .map(_ => SystemEvent(Prefix(ESW, "filter"), EventName(s"event_key_$streamId")))
//      .take(1000)

  def publishEvent(noOfPublishers: Int, noOfEvents: Int, every: FiniteDuration): Source[SystemEvent, NotUsed] = {
    Source(1 to noOfPublishers).flatMapMerge(noOfPublishers, i => publish(i, noOfEvents, every))
  }
}

package eng.arch.ingestor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object EventServiceMock {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(Behaviors.empty, "bla")

  def publish(streamId: Int, noOfEvents: Int, every: FiniteDuration): Source[SystemEvent, Future[Done]] = {
    Source.queue[SystemEvent](1024, OverflowStrategy.dropHead).mapMaterializedValue { q =>
      stream(streamId).throttle(noOfEvents, every).runForeach(q.offer)
    }
  }

  def createEvent(streamId: Int) =
    SystemEvent(Prefix(ESW, "filter"), EventName(s"event_key_$streamId"))
      .madd(
        StringKey.make(s"param_key_1").set(s"param-value-1"),
        StringKey.make(s"param_key_22").set(s"param-value-22"),
        StringKey.make(s"param_key_333").set(s"param-value-333")
      )

  def stream(streamId: Int) =
    Source
      .fromIterator(() => Iterator.from(1))
      .map(_ => createEvent(streamId))
//      .take(1000)

  def eventStream(noOfPublishers: Int, noOfEvents: Int, every: FiniteDuration): Source[SystemEvent, NotUsed] = {
    Source(1 to noOfPublishers).flatMapMerge(noOfPublishers, i => publish(i, noOfEvents, every))
  }
}

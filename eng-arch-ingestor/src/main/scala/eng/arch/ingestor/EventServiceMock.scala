package eng.arch.ingestor

import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class EventServiceMock(noOfPublishers: Int, eventsPerPublisher: Int, every: FiniteDuration)(implicit
    actorSystem: ActorSystem[_]
) {

  def subscribeAll(): Source[SystemEvent, NotUsed] = {
    Source(1 to noOfPublishers).flatMapMerge(
      noOfPublishers,
      publisherId => subscribe(publisherId)
    )
  }

  def subscribe(publisherId: Int): Source[SystemEvent, Future[Done]] = {
    Source.queue[SystemEvent](1024, OverflowStrategy.dropHead).mapMaterializedValue { redisQueue =>
      publisher(publisherId).runForeach(redisQueue.offer)
    }
  }

  def publisher(publisherId: Int): Source[SystemEvent, NotUsed] = {
    Source
      .fromIterator(() => Iterator.from(1))
      .map(_ => createEvent(publisherId))
      .throttle(eventsPerPublisher, every)
  }

  def createEvent(streamId: Int): SystemEvent =
    SystemEvent(Prefix(ESW, "filter"), EventName(s"event_key_$streamId"))
      .madd(
        StringKey.make(s"param_key_1").set(s"param-value-1"),
        StringKey.make(s"param_key_22").set(s"param-value-22"),
        StringKey.make(s"param_key_333").set(s"param-value-333")
      )
}

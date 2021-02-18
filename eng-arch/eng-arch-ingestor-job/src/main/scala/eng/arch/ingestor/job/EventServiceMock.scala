package eng.arch.ingestor.job

import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.{Prefix, Subsystem}
import eng.arch.ingestor.job.util.ParamSetData

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class EventServiceMock(noOfPublishers: Int, eventsPerPublisher: Int, every: FiniteDuration)(implicit
    actorSystem: ActorSystem[_]
) {

  def subscribeAll(): Source[SystemEvent, NotUsed] = {
    Source(1 to noOfPublishers).flatMapMerge(noOfPublishers, _ => subscribe())
  }

  def subscribe(): Source[SystemEvent, Future[Done]] = {
    Source.queue[SystemEvent](1024, OverflowStrategy.dropHead).mapMaterializedValue { redisQueue =>
      publisher().runForeach(redisQueue.offer)
    }
  }

  def publisher(): Source[SystemEvent, NotUsed] = {
    val subsystems = Iterator.from(1).flatMap(_ => Subsystem.values.iterator)
    val ids        = Iterator.from(1).flatMap(_ => 1 to 10)
    Source
      .fromIterator(() => Iterator.from(1))
      .map(_ => createEvent(subsystems.next(), ids.next()))
      .throttle(eventsPerPublisher, every)
  }

  def createEvent(subsystem: Subsystem, eventId: Int): SystemEvent = {
    SystemEvent(Prefix(subsystem, "filter"), EventName(s"event_key_$eventId")).madd(ParamSetData.paramSet)
  }
}

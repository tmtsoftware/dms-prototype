package metadata.util

import akka.actor.Cancellable
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source

import scala.concurrent.duration.FiniteDuration

object SourceUtil {
  def tick(initialDelay: FiniteDuration, every: FiniteDuration)(implicit system: ActorSystem[_]): Source[Unit, Cancellable] = {
    import system.executionContext
    // buffer size of the queue should be 0 so as to follow the semantics of Source.tick
    Source.queue[Unit](0, OverflowStrategy.dropHead).mapMaterializedValue { q =>
      val cancellable = system.scheduler.scheduleAtFixedRate(initialDelay, every)(() => q.offer(()))
      q.watchCompletion().onComplete(_ => cancellable.cancel())
      cancellable
    }
  }
}

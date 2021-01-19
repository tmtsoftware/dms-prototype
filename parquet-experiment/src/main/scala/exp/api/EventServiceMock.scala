package exp.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import csw.EventFactory

import scala.concurrent.duration.DurationInt

object EventServiceMock {
  def eventStream(): Source[SystemEventRecord, NotUsed] = {
    val eventIds = Iterator.from(1)

    Source
      .fromIterator(() => eventIds)
      .throttle(1, 1.millis)
      .map { eventId =>
        SystemEventRecord.generate(EventFactory.generateEvent(eventId))
      }
      .take(92000)
  }
}

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

  def captureSnapshot(exposureId: Long, obsEventName: String): Seq[SystemEventRecord] = {
    (1 to 2300).map(_ => SystemEventRecord.generate(EventFactory.generateEvent()))
  }

  def captureSnapshot2(exposureId: Long, obsEventName: String): Seq[SystemEventRecord2] = {
    (1 to 2300).map(_ => SystemEventRecord2.generate(exposureId, obsEventName, EventFactory.generateEvent()))
  }
}

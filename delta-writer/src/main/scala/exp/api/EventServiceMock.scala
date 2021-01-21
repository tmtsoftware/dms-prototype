package exp.api

import akka.NotUsed
import akka.stream.scaladsl.Source

object EventServiceMock {
  def eventStream(): Source[SystemEventRecord, NotUsed] = {
    val eventIds = Iterator.from(1)

    Source
      .fromIterator(() => eventIds)
      .map { eventId =>
        SystemEventRecord.generate(eventId.toString)
      }
      .take(220000)
  }

  def captureSnapshot(): Seq[SystemEventRecord] = {
    (1 to 2000).map(_ => SystemEventRecord.generate())
  }
}

package exp.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import csw.EventFactory
import csw.params.events.Event

object EventServiceMock {
  def eventRecordStream(): Source[SystemEventRecord, NotUsed] = {
    eventStream().map(event => SystemEventRecord.generate(event))
  }

  def eventStream(): Source[Event, NotUsed] = {
    Source
      .fromIterator(() => Iterator.from(1))
      .map(eventId => EventFactory.generateEvent(eventId))
      .take(200000)
  }
}

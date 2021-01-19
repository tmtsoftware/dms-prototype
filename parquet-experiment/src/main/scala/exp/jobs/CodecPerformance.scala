package exp.jobs

import csw.EventFactory
import csw.params.core.formats.ParamCodecs._
import csw.params.events.Event
import io.bullet.borer.Json

object CodecPerformance {

  def main(args: Array[String]): Unit = {
    (1 to 10).foreach { x =>
      val events: Seq[Event] = (1 to 4600).map(x => EventFactory.generateEvent())
      val start              = System.currentTimeMillis()
      val string             = Json.encode(events).toByteArray
      Json.decode(string).to[Seq[Event]].value
      val end = System.currentTimeMillis()
      println(end - start)
    }
  }

}

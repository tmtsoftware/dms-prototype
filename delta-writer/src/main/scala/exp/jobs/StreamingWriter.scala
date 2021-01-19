package exp.jobs

import akka.Done
import akka.actor.typed.ActorSystem
import exp.api.{EventServiceMock, SparkTable}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class StreamingWriter(sparkTable: SparkTable)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext

  def setup(): Future[Done] =
    Future {
//      sparkTable.delete()
//      println("deleted existing table")
      sparkTable.create()
      println("created new table")
      Done
    }

  def run(): Future[Done] = {

    EventServiceMock
      .eventStream()
      .groupedWithin(10000, 5.seconds)
      .mapAsync(1)(sparkTable.append)
      .run()
  }
}

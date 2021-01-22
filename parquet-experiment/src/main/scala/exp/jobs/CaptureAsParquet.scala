package exp.jobs

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import exp.api.EventServiceMock
import exp.writer.ParquetIO

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object CaptureAsParquet {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val parquetIO = new ParquetIO("target/data/parquet")

    EventServiceMock
      .eventStream()
      .groupedWithin(10000, 5.seconds)
      .mapAsync(1) { batch =>
        parquetIO.write(batch).map(_ => batch.length)
      }
      .statefulMapConcat { () =>
        var start = System.currentTimeMillis()
        batchSize =>
          val current = System.currentTimeMillis()
          println(s"Finished writing batch size $batchSize in ${current - start} milliseconds >>>>>>>>>>>>>>>>>>>>")
          start = current
          List(batchSize)
      }
      .run()
      .onComplete { x =>
        actorSystem.terminate()
        x match {
          case Failure(exception) => exception.printStackTrace()
          case Success(value)     =>
        }
      }
  }
}

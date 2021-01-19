package exp.jobs

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.github.mjakubowski84.parquet4s._
import exp.api.{Constants, SystemEventRecord}
import org.apache.hadoop.conf.Configuration

import scala.util.{Failure, Success}

object ParquetStreamingReaderJob2 {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    ParquetStreams
      .fromParquet[SystemEventRecord]
      .read(Constants.StreamingDir)
      .runForeach(println)
      .onComplete { x =>
        actorSystem.terminate()
        x match {
          case Failure(exception) => exception.printStackTrace()
          case Success(value)     =>
        }
      }
  }
}

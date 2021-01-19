package exp.jobs

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.github.mjakubowski84.parquet4s._
import exp.api.Constants
import org.apache.hadoop.conf.Configuration

import scala.util.{Failure, Success}

object ParquetStreamingReaderJob {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    ParquetStreams
      .fromParquet[Projection]
      .withProjection
      .withFilter(
        (Col("exposureId") === "2" || Col("exposureId") === "6") &&
          Col("nanos") >= 638299000L &&
          Col("nanos") <= 638499000L
      )
      .withOptions(ParquetReader.Options(hadoopConf = new Configuration()))
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

case class Projection(eventId: String, nanos: Long, eventTime: String)

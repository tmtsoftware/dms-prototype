package exp.writer

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.github.mjakubowski84.parquet4s.{ParquetStreams, ParquetWriter}
import exp.api.EventServiceMock
import org.apache.parquet.hadoop.metadata.CompressionCodecName

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object StreamWIP {
  val writeOptions: ParquetWriter.Options = ParquetWriter.Options(
    compressionCodecName = CompressionCodecName.SNAPPY
  )

  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    EventServiceMock
      .eventRecordStream()
      .via(
        ParquetStreams
          .viaParquet("target/data/parquet2")
          .withMaxCount(10000)
          .withMaxDuration(5.seconds)
//          .withWriteOptions(writeOptions)
//          .withPartitionBy("date", "hour", "minute")
          .build()
      )
      .statefulMapConcat { () =>
        var start = System.currentTimeMillis()

        eventRecord =>
          val count = eventRecord.eventId.toInt
          if (count % 100 == 0) {
            val current = System.currentTimeMillis()
            println(s"Finished writing items: $count in ${current - start} milliseconds >>>>>>>>>>>>>>>>>>>>")
            start = current
          }
          List(eventRecord)
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

package exp.jobs

import java.io.File

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.github.mjakubowski84.parquet4s.{ParquetStreams, ParquetWriter}
import exp.api.{Constants, EventServiceMock}
import org.apache.commons.io.FileUtils
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object ParquetStreamingWriterJobWithSwapping {
  val writeOptions: ParquetWriter.Options = ParquetWriter.Options(
    writeMode = ParquetFileWriter.Mode.CREATE,
    compressionCodecName = CompressionCodecName.SNAPPY
//    hadoopConf = {
//      val dd = new Configuration()
//      dd.set("fs.s3a.endpoint", "http://localhost:9000")
//      dd.set("fs.s3a.access.key", "minioadmin")
//      dd.set("fs.s3a.secret.key", "minioadmin")
//      dd.set("fs.s3a.path.style.access", "true")
//      dd.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
//      dd
//    }
  )

  private val file = new File(Constants.StreamingDir)
  if (file.exists()) {
    FileUtils.deleteDirectory(file)
    println("deleted the existing table")
  }

  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    var previousMinute = "invalid"
    EventServiceMock
      .eventStream()
      .via(
        ParquetStreams
          .viaParquet(Constants.StreamingDir)
          .withMaxCount(1000)
          .withMaxDuration(5.seconds)
          .withWriteOptions(writeOptions)
          .withPartitionBy("hour", "minute")
          .build()
      )
      .statefulMapConcat { () =>
        var start = System.currentTimeMillis()

        eventRecord =>
          if (previousMinute == "invalid") previousMinute = eventRecord.minute
          if (previousMinute != eventRecord.minute) {
            val dataDir = "target/data"
            val fromDir = s"$dataDir/parquet-streams/hour=${eventRecord.hour}/minute=$previousMinute"
            val toDir   = s"$dataDir/temp/hour=${eventRecord.hour}/minute=$previousMinute"
            println(s"Moving - $fromDir to $toDir")
            FileUtils.moveDirectory(new File(fromDir), new File(toDir))
            println("Done")
            previousMinute = eventRecord.minute
          }
          val count = eventRecord.eventId.toInt
          if (count % 1000 == 0) {
            println("minute " + eventRecord.minute)
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

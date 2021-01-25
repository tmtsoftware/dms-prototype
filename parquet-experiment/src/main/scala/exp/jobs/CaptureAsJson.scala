package exp.jobs

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import exp.api.EventServiceMock
import exp.writer.JsonIO
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object CaptureAsJson {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val conf                   = new Configuration
    val fileSystem: FileSystem = FileSystem.get(new URI("file:///"), conf)

    val jsonIO = new JsonIO("target/data/json", fileSystem)

    EventServiceMock
      .eventStream()
      .groupedWithin(10000, 5.seconds)
      .mapAsync(1) { batch =>
        jsonIO.writeHdfs(batch).map(_ => batch.length)
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
          case Failure(exception) =>
            exception.printStackTrace()
            fileSystem.close()
          case Success(value) =>
            fileSystem.close()
        }
      }
  }
}

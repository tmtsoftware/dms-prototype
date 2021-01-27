package eng.arch.ingestor

import java.net.URI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import eng.arch.ingestor.util.JsonIO
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object JsonIngestor {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val conf = new Configuration
//    val fileSystem: FileSystem = FileSystem.get(new URI("file:///"), conf)
//    val jsonIO                 = new JsonIO("target/data/json", fileSystem)
    val fileSystem: FileSystem = FileSystem.get(new URI("hdfs://localhost:9000/"), conf)
    val jsonIO                 = new JsonIO("hdfs://localhost:9000/" + "/data/json", fileSystem)

    val startTime = System.currentTimeMillis()
    EventServiceMock
      .eventStream(100, 1, 10.millis)
      .take(500000)
      .groupedWithin(10000, 5.seconds)
      .mapAsync(1) { batch =>
        println("writing")
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
        x match {
          case Failure(exception) => exception.printStackTrace()
          case Success(value)     => print(s"TOTAL TIME ${System.currentTimeMillis() - startTime}")
        }
        actorSystem.terminate()
        EventServiceMock.system.terminate()
      }
  }
}

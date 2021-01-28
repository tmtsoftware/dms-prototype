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
    val fileSystem: FileSystem = FileSystem.get(new URI("hdfs://localhost:8020/"), conf)
    val jsonIO                 = new JsonIO("/tmp/json", fileSystem)

    val eventServiceMock = new EventServiceMock(noOfPublishers = 100, eventsPerPublisher = 1, every = 10.millis)

    val startTime = System.currentTimeMillis()
    eventServiceMock
      .subscribeAll()
      .take(100000)
      .groupedWithin(10000, 5.seconds)
      .mapAsync(4) { batch =>
        val start = System.currentTimeMillis()
        jsonIO.writeHdfs(batch).map { _ =>
          val current = System.currentTimeMillis()
          println(s"Finished writing batch size ${batch.length} in ${current - start} milliseconds >>>>>>>>>>>>>>>>>>>>")
        }
      }
      .run()
      .onComplete { x =>
        x match {
          case Failure(exception) => exception.printStackTrace()
          case Success(value)     => print(s"TOTAL TIME ${System.currentTimeMillis() - startTime}")
        }
        actorSystem.terminate()
      }
  }
}

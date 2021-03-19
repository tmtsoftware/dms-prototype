package eng.arch.ingestor.job

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import eng.arch.ingestor.job.util.JsonIO
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

import java.net.URI
import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object JsonIngestor {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val conf                   = new Configuration
    val fileSystem: FileSystem = FileSystem.get(new URI("file:///"), conf)
    val jsonIO                 = new JsonIO("target/data/json", fileSystem)

    val eventServiceMock =
      new EventServiceMock(noOfPublishers = 100, eventsPerPublisher = 2, every = 10.millis) // 20k events/second

    println("START TIME :" + Instant.now)
    val startTime              = System.currentTimeMillis()
    val eventCountFor128MbFile = 1100000
    val eventCountFor1GbFile   = eventCountFor128MbFile * 8
    val numberOfEvents         = 1
    val numberOf1GB            = 1

    eventServiceMock
      .subscribeAll()
//      .take(eventCountFor1GbFile * numberOfEvents * numberOf1GB)
      .groupedWithin(40000, 2.seconds) // 2 seconds data of 20k events/second
      .mapAsync(4) { batch =>
        val start = System.currentTimeMillis()
        jsonIO.write(batch).map { _ =>
          val current = System.currentTimeMillis()
          println(s"Finished writing batch size ${batch.length} in ${current - start} milliseconds >>>")
        }
      }
      .run()
      .onComplete { x =>
        x match {
          case Failure(exception) => exception.printStackTrace()
          case Success(value)     => print(s"TOTAL TIME ${System.currentTimeMillis() - startTime} :: ${Instant.now}")
        }
        actorSystem.terminate()
      }
  }
}

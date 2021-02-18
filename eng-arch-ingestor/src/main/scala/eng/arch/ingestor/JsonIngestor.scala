package eng.arch.ingestor

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

    val startTime = System.currentTimeMillis()

    eventServiceMock
      .subscribeAll()
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

package dms.eng.archive

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import dms.eng.archive.core.EventGlobalSubscriber
import dms.eng.archive.util.JsonIO
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object CaptureAsJson {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val eventServicePort         = 26379
    val host                     = "localhost"
    val redisClient: RedisClient = RedisClient.create()
    val redisURI: RedisURI       = RedisURI.Builder.sentinel(host, eventServicePort, "eventServer").build()

    val globalSubscriber = new EventGlobalSubscriber(redisURI, redisClient)
    val jsonIO           = new JsonIO("target/data/json")
    val startTime        = System.currentTimeMillis()

    globalSubscriber
      .subscribeAll()
      .take(15000)
      .groupedWithin(10000, 5.seconds)
      .mapAsync(1) { batch =>
        jsonIO.write(batch).map(_ => batch.length)
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
          case Success(value)     => print(s"TOTAL TIME ${System.currentTimeMillis() - startTime}")
        }
      }
  }
}

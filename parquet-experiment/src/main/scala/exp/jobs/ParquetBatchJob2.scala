package exp.jobs

import java.io.File

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import csw.params.core.generics.Parameter
import exp.api.{Constants, EventServiceMock}
import io.bullet.borer.Json
import org.apache.commons.io.FileUtils
import csw.params.core.formats.ParamCodecs._

import scala.util.{Failure, Success}

object ParquetBatchJob2 {
  val exposureIds: Seq[Int]      = (1 to 10)
  val obsEventNames: Seq[String] = List("eventStart", "eventEnd")

  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val table = new File(Constants.BatchingDir)

    if (table.exists()) {
      FileUtils.deleteDirectory(table)
      println("deleted the existing table")
    }

    val parquetSnapshotIo = new ParquetSnapshotIO(Constants.BatchingDir)

    def write() = {
      Source(exposureIds)
        .flatMapConcat { expId =>
          Source(obsEventNames).mapAsync(1) { obsName =>
            val start = System.currentTimeMillis()
            parquetSnapshotIo.write(EventServiceMock.captureSnapshot(expId, obsName)).map { _ =>
              val current = System.currentTimeMillis()
              println(s"Finished writing items in ${current - start} milliseconds >>>>>>>>>>>>>>>>>>")
            }
          }
        }
        .run()
    }

    def read() = {
      Source(exposureIds)
        .mapAsync(1) { expId =>
          val start = System.currentTimeMillis()
          parquetSnapshotIo.read[BatchProjection](expId.toString).map { batch =>
            batch.foreach(x => Json.decode(x.paramSet.getBytes()).to[Set[Parameter[_]]].value)
            val current = System.currentTimeMillis()
            println(s"Finished reading ${batch.length} items in ${current - start} milliseconds <<<<<<<<<<<<<<<<<")
          }
        }
        .run()
    }

    write().flatMap(_ => read()).onComplete { x =>
      actorSystem.terminate()
      x match {
        case Failure(exception) => exception.printStackTrace()
        case Success(value)     =>
      }
    }
  }

}

case class BatchProjection(paramSet: String)

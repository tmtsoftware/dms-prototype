package db
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import csw.params.core.generics.Parameter
import csw.params.core.formats.ParamCodecs._
import exp.api.{EventServiceMock, SystemEventRecord, SystemEventRecord2}
import io.bullet.borer.{Cbor, Json}
import scalikejdbc._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object DbMain {

  val exposureIds: Seq[Int]      = (1 to 10)
  val obsEventNames: Seq[String] = List("eventStart", "midEvent", "eventEnd")

  def main(args: Array[String]): Unit = {
    Class.forName("org.postgresql.Driver")
    ConnectionPool.singleton("jdbc:postgresql://localhost:5432/postgres", "postgres", "")

    implicit val session: AutoSession              = AutoSession
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")

    val dbIO = new DbIO()

    dbIO.setup()

    exposureIds.foreach { expId =>
      obsEventNames.foreach { obsName =>
        val start   = System.currentTimeMillis()
        val records = EventServiceMock.captureSnapshot2(expId, obsName)
        dbIO.batchWrite(records)
        val current = System.currentTimeMillis()
        println(s"Finished writing items in ${current - start} milliseconds >>>>>>>>>>>>>>>>>>")
      }
    }

    exposureIds.foreach { expId =>
      val start     = System.currentTimeMillis()
      val paramSets = dbIO.read(expId.toString)
      paramSets.foreach(x => Json.decode(x).to[Set[Parameter[_]]].value)
      val current = System.currentTimeMillis()
      println(s"Finished reading ${paramSets.length} items in ${current - start} milliseconds <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
    }

    session.close()
    actorSystem.terminate()
  }

}

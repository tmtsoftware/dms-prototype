package metadata.db

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.client.ActorSystemFactory
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object PersistApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  implicit val context: DSLContext                        = DbSetup.dslContext
  private val dbUtil                                      = new DbUtil(context)

  private val snapshotTable = "event_snapshots"
  Await.result(DbSetup.dropTable(snapshotTable), 5.seconds)
  Await.result(DbSetup.createTable(snapshotTable), 5.seconds)

  private val event: SystemEvent = SystemEvent(Prefix(ESW, "filter"), EventName("wheel5"))
  private val exposures          = List("exposureStart", "exposureMiddle", "exposureEnd")

  (1 to 20).foreach { i =>
    exposures.map { obsEventName =>
      val startTime = System.currentTimeMillis()
      val snapshot  = EventService.createSnapshot(s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", obsEventName, event)
      Await.result(dbUtil.batchInsertParallel(snapshotTable, snapshot.values.toList), 5.seconds)
      println(
        s"items: ${snapshot.size}, time : ${System.currentTimeMillis() - startTime} millis >>>>>>>>>>>writing>>>>>>>>>>>>>"
      )
    }
  }

  system.terminate()
}

package metadata.db

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.client.ActorSystemFactory
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import metadata.snapshot.processor.SnapshotProcessorUtil
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object PersistHeaderKeywords extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  implicit val context: DSLContext = DbSetup.dslContext
  private val dbUtil               = new DbUtil(context)

  private val snapshotTable    = "event_snapshots"
  private val headersDataTable = "headers_data"
  Await.result(DbSetup.dropTable(snapshotTable), 5.seconds)
  Await.result(DbSetup.dropTable(headersDataTable), 5.seconds)
  Await.result(DbSetup.createTable(snapshotTable), 5.seconds)
  Await.result(DbSetup.createHeadersDataTable(headersDataTable), 5.seconds)

  private val event     = SystemEvent(Prefix(ESW, "filter"), EventName("wheel5"))
  private val exposures = List("exposureStart", "exposureMiddle", "exposureEnd")

  private val counter = 10
  (1 to counter).foreach { i =>
    exposures.foreach { obsEventName =>
      val startTime = System.currentTimeMillis()

      //CAPTURE SNAPSHOT
      val snapshot: Map[EventKey, EventRecord] =
        EventService.createSnapshot(s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", obsEventName, event)

      //PERSIST SNAPSHOT
      Await.result(dbUtil.batchInsertParallel(snapshotTable, snapshot.values.toList), 5.seconds)

      //PERSIST KEYWORDS
      val headersValueMap: Map[String, String] = SnapshotProcessorUtil.getHeaderData1(snapshot)
      Await.result(
        dbUtil.batchAsyncHeaderData(headersDataTable, s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", obsEventName, headersValueMap),
        5.seconds
      )

      println(
        s"Rows: ${snapshot.size}, Headers: ${headersValueMap.size}, time : ${System.currentTimeMillis() - startTime} millis >>>>>>>>>>>writing>>>>>>>>>>>>>"
      )
    }

  }

  def queryHeaders(expId: String, dslContext: DSLContext, tableName: String)(implicit executionContext: ExecutionContext) = {
    val getDatabaseQuery =
      dslContext.resultQuery(
        s"select * from $tableName where exposure_id='$expId'"
      )

    val headerData    = getDatabaseQuery.fetchAsyncScala[(String, String, String, String)]
    val headersFromDb = Await.result(headerData, 10.seconds)
    SnapshotProcessorUtil.generateFormattedHeader(headersFromDb.map(h => (h._3 -> Some(h._4))).toMap)
    headersFromDb
  }

  (1 to counter).foreach { i =>
    val startTime = System.currentTimeMillis()

    val headers = queryHeaders(s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", context, headersDataTable)
    println(
      s"Headers: ${headers.size}, time : ${System.currentTimeMillis() - startTime} millis <<<<<<<<<<<<<<<<reading<<<<<<<<<<<<<<<<"
    )
  }
}

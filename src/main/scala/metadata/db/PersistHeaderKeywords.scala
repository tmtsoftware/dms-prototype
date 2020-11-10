package metadata.db

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS}
import metadata.snapshot.processor.SnapshotProcessorUtil
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.jdk.CollectionConverters.CollectionHasAsScala

object PersistHeaderKeywords extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  implicit val context: DSLContext = DbSetup.dslContext
  private val dbUtil               = new DbUtil(context)

  private val snapshotTable    = "event_snapshots"
  private val headersDataTable = "keyword_values"
  Await.result(DbSetup.dropTable(snapshotTable), 5.seconds)
  Await.result(DbSetup.dropTable(headersDataTable), 5.seconds)
  Await.result(DbSetup.createTable(snapshotTable, "text"), 5.seconds)
  Await.result(DbSetup.createHeadersDataTable(headersDataTable), 5.seconds)

  private val prefix    = Prefix(ESW, "filter")
  private val event     = SystemEvent(prefix, EventName("wheel5"))
  private val exposures = List("exposureStart", "exposureMiddle", "exposureEnd")

  private val counter = 10
  (1 to counter).foreach { i =>
    exposures.foreach { obsEventName =>
      val startTime = System.currentTimeMillis()

      val expId = s"2034A-P054-O010-WFOS-BLU1-SCI1-$i"
      //CAPTURE SNAPSHOT
      val snapshot: ConcurrentHashMap[EventKey, Event] =
        EventService.createSnapshot(event)

      //PERSIST SNAPSHOT
      val snapshotFuture = dbUtil.batchInsertSnapshots(expId, obsEventName, snapshot.values().asScala.toList, snapshotTable)

      //PERSIST KEYWORDS
      val headersValues: List[(String, Option[String])] = SnapshotProcessorUtil.getHeaderData1(obsEventName, snapshot)
      val KeywordsFuture =
        dbUtil.batchInsertHeaderData(headersDataTable, s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", obsEventName, headersValues)

      Await.result(
        snapshotFuture zip KeywordsFuture,
        5.seconds
      )

      println(
        s"Rows: ${snapshot.size}, Headers: ${headersValues.size}, time : ${System.currentTimeMillis() - startTime} millis >>>>>>>>>>>writing>>>>>>>>>>>>>"
      )
    }

  }

  def queryHeaders(expId: String, dslContext: DSLContext, keywords: Seq[String], tableName: String)(implicit
      executionContext: ExecutionContext
  ) = {
    val getDatabaseQuery =
      dslContext.resultQuery(
        s"select * from $tableName where exposure_id='$expId'"
      )

    val headerData    = getDatabaseQuery.fetchAsyncScala[(String, String, String, String)]
    val headersFromDb = Await.result(headerData, 10.seconds)
    val formattedHeaders =
      SnapshotProcessorUtil.generateFormattedHeader(keywords, headersFromDb.map(h => (h._3 -> Some(h._4))).toMap)
    formattedHeaders
  }

  val keywords: Seq[String] = SnapshotProcessorUtil.readHeaderList()(IRIS)

  (1 to counter).foreach { i =>
    val startTime = System.currentTimeMillis()

    val headersFromDb: String = queryHeaders(s"2034A-P054-O010-WFOS-BLU1-SCI1-$i", context, keywords, headersDataTable)
//    println(headersFromDb)
    println(
      s"Headers: ${headersFromDb.lines().count()}, time : ${System.currentTimeMillis() - startTime} millis <<<<<<<<<<<<<<<<reading<<<<<<<<<<<<<<<<"
    )
  }
}

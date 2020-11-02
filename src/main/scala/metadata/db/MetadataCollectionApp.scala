package metadata.db

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Subsystem.{ESW, IRIS}
import csw.prefix.models.{Prefix, Subsystem}
import metadata.snapshot.processor.SnapshotProcessorUtil.loadHeaderConfig
import metadata.snapshot.processor.{HeaderConfig, SnapshotProcessorUtil}
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.jdk.CollectionConverters.CollectionHasAsScala

object MetadataCollectionApp extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  implicit val dslContext: DSLContext =
    Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)
  private val dbUtil = new DbUtil(dslContext)

  private val snapshotTable    = "event_snapshots"
  private val headersDataTable = "headers_data"
  Await.result(DbSetup.dropTable(snapshotTable), 5.seconds)
  Await.result(DbSetup.dropTable(headersDataTable), 5.seconds)
  Await.result(DbSetup.createTable(snapshotTable, "text"), 5.seconds)
  Await.result(DbSetup.createHeadersDataTable(headersDataTable), 5.seconds)

  private val prefix    = Prefix(ESW, "filter")
  private val event     = SystemEvent(prefix, EventName("wheel5"))
  private val exposures = List("exposureStart", "exposureMiddle", "exposureEnd")

  val headerConfigs: Map[Subsystem, List[HeaderConfig]] = loadHeaderConfig()

  private val counter              = 10
  private val subsystem: Subsystem = IRIS

  (1 to counter).foreach { i =>
    exposures.foreach { obsEventName =>
      val startTime = System.currentTimeMillis()

      val expId = s"2034A-P054-O010-${subsystem.name}-BLU1-SCI1-$i"
      //CAPTURE SNAPSHOT
      val snapshot: ConcurrentHashMap[EventKey, Event] =
        EventService.createSnapshot(event)

      //PERSIST SNAPSHOT
      Await.result(
        dbUtil.batchInsertParallelSnapshots(expId, obsEventName, snapshot.values().asScala.toList, snapshotTable),
        5.seconds
      )

      //PERSIST KEYWORDS
      val headersValues: List[(String, Option[String])] =
        SnapshotProcessorUtil.getHeaderData(obsEventName, snapshot, headerConfigs(subsystem))
      Await.result(
        dbUtil.batchInsertHeaderData(headersDataTable, expId, obsEventName, headersValues),
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

  val keywords: Seq[String] = SnapshotProcessorUtil.loadHeaderList()(subsystem)

  (1 to counter).foreach { i =>
    val startTime = System.currentTimeMillis()

    val expId                 = s"2034A-P054-O010-${subsystem.name}-BLU1-SCI1-$i"
    val headersFromDb: String = queryHeaders(expId, dslContext, keywords, headersDataTable)
//    println(headersFromDb)
    println(
      s"Headers: ${headersFromDb.lines().count()}, time : ${System.currentTimeMillis() - startTime} millis <<<<<<<<<<<<<<<<reading<<<<<<<<<<<<<<<<"
    )
  }
}

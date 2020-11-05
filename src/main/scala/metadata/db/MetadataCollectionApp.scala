package metadata.db

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import csw.location.client.ActorSystemFactory
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Subsystem.{ESW, IRIS}
import csw.prefix.models.{Prefix, Subsystem}
import dms.metadata.collection.Keyword.{KeywordConfig, KeywordValueExtractor}
import dms.metadata.collection.Keyword.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}
import metadata.snapshot.processor.SnapshotProcessorUtil.loadHeaderConfig
import metadata.snapshot.processor.SnapshotProcessorUtil
import metadata.util.DbUtil
import org.jooq.DSLContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object MetadataCollectionApp extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  implicit val dslContext: DSLContext =
    Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)
  private val dbUtil = new DbUtil(dslContext)

  private val snapshotTable    = "event_snapshots"
  private val headersDataTable = "keyword_values"
  Await.result(DbSetup.dropTable(snapshotTable), 5.seconds)
  Await.result(DbSetup.dropTable(headersDataTable), 5.seconds)
  Await.result(DbSetup.createTable(snapshotTable, "text"), 5.seconds)
  Await.result(DbSetup.createHeadersDataTable(headersDataTable), 5.seconds)
  Await.result(DbSetup.createIndex(snapshotTable, "idx_exposure_id", "exposure_id"), 5.seconds)
  Await.result(DbSetup.createIndex(headersDataTable, "idx_hdr_exposure_id", "exposure_id"), 5.seconds)

  private val prefix    = Prefix(ESW, "filter")
  private val event     = SystemEvent(prefix, EventName("wheel5"))
  private val exposures = List("exposureStart", "exposureMiddle", "exposureEnd")

  val headerConfigs: Map[Subsystem, List[KeywordConfig]] = loadHeaderConfig()

  private val counter              = 10
  private val subsystem: Subsystem = IRIS

  private val snapshotProcessor = new KeywordValueExtractor

  (1 to counter).foreach { i =>
    exposures.foreach { obsEventName =>
      val startTime = System.currentTimeMillis()

      val expId = s"2034A-P054-O010-${subsystem.name}-BLU1-SCI1-$i"
      //CAPTURE SNAPSHOT
      val snapshot: ConcurrentHashMap[EventKey, Event] = EventServiceUtil.createSnapshot(event)

      //PERSIST SNAPSHOT
      Await.result(
        dbUtil.batchInsertSnapshots(expId, obsEventName, snapshot, snapshotTable),
        5.seconds
      )

      //PERSIST KEYWORDS
      val headersValues: Map[String, String] = headerConfigs(subsystem)
        .filter(_.obsEventName == obsEventName)
        .map {
          case x: ComplexKeywordConfig               => (x.keyword, snapshotProcessor.extract(x, snapshot))
          case ConstantKeywordConfig(keyword, value) => (keyword, value)
        }
        .toMap

      Await.result(
        dbUtil.batchInsertHeaderData(headersDataTable, expId, headersValues),
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

    val headerData    = getDatabaseQuery.fetchAsyncScala[(String, String, String)]
    val headersFromDb = Await.result(headerData, 10.seconds)
    val formattedHeaders =
      SnapshotProcessorUtil.generateFormattedHeader(keywords, headersFromDb.map(h => h._2 -> Some(h._3)).toMap)
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

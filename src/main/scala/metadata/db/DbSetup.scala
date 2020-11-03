package metadata.db

import csw.database.scaladsl.JooqExtentions.RichQuery
import org.jooq.DSLContext

import scala.concurrent.Future

object DbSetup {
  def createTable(table: String, paramSetType: String = "bytea")(implicit ctx: DSLContext): Future[Integer] =
    ctx
      .query(s"""
         |CREATE TABLE $table (
         |  exposure_id VARCHAR(50) NOT NULL,
         |  obs_event_name VARCHAR(50) NOT NULL,
         |  source VARCHAR(50) NOT NULL,
         |  eventName VARCHAR(50) NOT NULL,
         |  eventId VARCHAR(50) NOT NULL,
         |  eventTime TIMESTAMP NOT NULL,
         |  paramSet $paramSetType
         |);
         |""".stripMargin)
      .executeAsyncScala()

  def createHeadersDataTable(table: String)(implicit ctx: DSLContext): Future[Integer] =
    ctx
      .query(s"""
                |CREATE TABLE $table (
                |  exposure_id VARCHAR(50) NOT NULL,
                |  keyword VARCHAR(50) NOT NULL,
                |  value VARCHAR(50) NOT NULL
                |);
                |""".stripMargin)
      .executeAsyncScala()

  def dropTable(table: String)(implicit ctx: DSLContext): Future[Integer] =
    ctx.query(s"DROP TABLE IF EXISTS $table").executeAsyncScala()

  def createIndex(table: String, index: String, columnToIndex: String)(implicit ctx: DSLContext): Future[Integer] = {
    ctx.query(s"CREATE INDEX $index ON $table ($columnToIndex)").executeAsyncScala()
  }

  def cleanTable(table: String)(implicit ctx: DSLContext): Future[Integer] =
    ctx.query(s"DELETE FROM $table").executeAsyncScala()
}

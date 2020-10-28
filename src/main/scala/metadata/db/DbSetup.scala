package metadata.db

import java.util.Properties

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import csw.database.scaladsl.JooqExtentions.RichQuery
import org.jooq.{DSLContext, SQLDialect}
import org.jooq.impl.DSL

import scala.concurrent.Future

object DbSetup {
  private val properties = new Properties()
  properties.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
  properties.setProperty("dataSource.serverName", "localhost")
  properties.setProperty("dataSource.portNumber", "5432")
  properties.setProperty("dataSource.databaseName", "postgres")
  properties.setProperty("dataSource.user", "postgres")
  private val hikariConfig = new HikariConfig(properties)

  def dslContext: DSLContext = DSL.using(new HikariDataSource(hikariConfig), SQLDialect.POSTGRES)

  def createTable(table: String, paramSetType: String = "bytea")(implicit ctx: DSLContext): Future[Integer] =
    dslContext
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
    dslContext
      .query(s"""
                |CREATE TABLE $table (
                |  exposure_id VARCHAR(50) NOT NULL,
                |  obs_event_name VARCHAR(50) NOT NULL,
                |  keyword VARCHAR(50) NOT NULL,
                |  value VARCHAR(50) NOT NULL
                |);
                |""".stripMargin)
      .executeAsyncScala()

  def dropTable(table: String)(implicit ctx: DSLContext): Future[Integer] =
    dslContext.query(s"DROP TABLE IF EXISTS $table").executeAsyncScala()

  def cleanTable(table: String)(implicit ctx: DSLContext): Future[Integer] =
    dslContext.query(s"DELETE FROM $table").executeAsyncScala()
}

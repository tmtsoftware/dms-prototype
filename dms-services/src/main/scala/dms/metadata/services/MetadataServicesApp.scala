package dms.metadata.services

import caseapp.RemainingArgs
import caseapp.core.app.CommandApp
import dms.metadata.access.AccessMain
import dms.metadata.collection.CollectionMain
import dms.metadata.services.cli.Command
import dms.metadata.services.cli.Command.Start

import scala.io.Source
import scala.sys.process._

object MetadataServicesApp extends CommandApp[Command] {

  override def run(command: Command, remainingArgs: RemainingArgs): Unit =
    command match {
      case s: Start => run(s)
    }

  def run(start: Start): Unit = {

    if (start.init) {
      println("initializing database")
      val sqlQueries  = Source.fromResource("db_setup.sql").getLines().mkString
      val psqlCommand = s"""psql -h localhost -d postgres -a -c "$sqlQueries" """
      psqlCommand.!!
    }

    if (start.collection) {
      println("starting collection service")
      CollectionMain.start(start.keywordMappingsConf)
    }

    if (start.access) {
      println("starting access service")
      AccessMain.start(start.port, start.keywordsConf)
    }
  }
}

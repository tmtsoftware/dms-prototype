package dms.metadata.services

import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import dms.metadata.access.AccessServiceApp
import dms.metadata.collection.CollectionApp
import dms.metadata.services.Command.Start

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
      val psqlCommand = s"""psql -d postgres -a -c "$sqlQueries" """
      psqlCommand.!!
    }

    if (start.collection) {
      println("starting collection service")
      CollectionApp.main(Array())
    }

    if (start.access) {
      println("starting access service")
      AccessServiceApp.main(Array(start.port.toString))
    }
  }
}

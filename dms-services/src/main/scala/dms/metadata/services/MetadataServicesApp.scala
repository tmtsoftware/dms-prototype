package dms.metadata.services

import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import dms.metadata.access.AccessServiceApp
import dms.metadata.collection.CollectionApp
import dms.metadata.services.Command.Start

object MetadataServicesApp extends CommandApp[Command] {

  override def run(command: Command, remainingArgs: RemainingArgs): Unit =
    command match {
      case s: Start => run(s)
    }

  def run(start: Start): Unit = {

    //    automate db setup
    if (start.init) {
      println("initializing database")

      val createUserSqlPath: String  = getClass.getResource("/create_user.sql").getPath
      val createTableSqlPath: String = getClass.getResource("/create_tables.sql").getPath

      //    Create user
      Runtime.getRuntime.exec(s"psql -d postgres -a -f $createUserSqlPath")

      //    Create table using created user
      Runtime.getRuntime.exec(s"psql -d postgres -U dmsuser -a -f $createTableSqlPath")
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

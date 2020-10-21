package metadata2

import cats.effect.{IO, Resource}
import skunk._
import skunk.implicits._

object DbSetup {
  private def createTable(name: String, paramType: String) =
    sql"""CREATE TABLE #$name(
           exposure_id    VARCHAR(50),
           obs_event_name VARCHAR(50),
           source         VARCHAR(50),
           eventName      VARCHAR(50),
           eventId        VARCHAR(50),
           eventTime      TIMESTAMP  ,
           paramSet       #$paramType
         )""".command

  private def dropTable(name: String) = sql"DROP TABLE #$name".command

  private def mkTableResource(s: Session[IO], name: String, paramType: String) = {
    val table = s.execute(createTable(name, paramType)).void
    val drop  = s.execute(dropTable(name)).void
    Resource.make(table)(_ => drop)
  }

  def withSnapshotTables(s: Session[IO]): Resource[IO, Unit] =
    mkTableResource(s, "event_snapshots", "varchar")
      .flatMap(_ => mkTableResource(s, "event_snapshots2", "bytea"))
}

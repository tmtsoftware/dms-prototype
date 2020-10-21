package metadata2

import cats.effect._
import cats.syntax.all._
import natchez.Trace.Implicits.noop
import skunk._

object Main extends IOApp {
  import DbSetup._
  import Utils._

  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "postgres",
      database = "postgres"
    )

  def run[T](times: Int, block: Int => IO[T]): IO[Unit] = (1 to times).map(id => measure(block(id))).toList.sequence_

  def run(args: List[String]): IO[ExitCode] =
    session.flatTap(withSnapshotTables).flatMap(MetadataService.fromSession(_)).use { s =>
      for {
        _ <- pprint("Inserting with Text encoding ...")
        _ <- run(10, id => s.insertSnapshot(SampleData.snapshot(id)))
        _ <- pprint("Reading Text encoded ...")
        _ <- run(10, id => s.getSnapshot(id.toString).compile.toList)

        _ <- pprint("Inserting with bytea encoding ...")
        _ <- run(10, id => s.insertSnapshot2(SampleData.snapshot2(id)))

        _ <- pprint("Reading bytea encoded [all columns]...")
        _ <- run(10, id => s.getSnapshot2(id.toString).compile.toList)

        _ <- pprint("Reading bytea encoded [paramSet column]...")
        _ <- run(10, id => s.getParamSet(id.toString).compile.toList)
      } yield ExitCode.Success
    }

}

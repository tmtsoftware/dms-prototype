package metadata2

import cats.effect._
import cats.syntax.all._
import natchez.Trace.Implicits.noop
import skunk._
import skunk.implicits._

object Main extends IOApp {
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "postgres",
      database = "postgres"
    )

  import scala.concurrent.duration.MILLISECONDS

  def measure[F[_], A](fa: F[A])(implicit F: Sync[F], clock: Clock[F]): F[(A, Long)] =
    for {
      start  <- clock.monotonic(MILLISECONDS)
      result <- fa
      finish <- clock.monotonic(MILLISECONDS)
    } yield (result, finish - start)

  private val createTable =
    sql"""CREATE TABLE event_snapshots(
           exposure_id    VARCHAR(50) NOT NULL,
           obs_event_name VARCHAR(50) NOT NULL,
           source         VARCHAR(50) NOT NULL,
           eventName      VARCHAR(50) NOT NULL,
           eventId        VARCHAR(50) NOT NULL,
           eventTime      TIMESTAMP   NOT NULL,
           paramSet       varchar
         )""".command
  private val dropTable = sql"DROP TABLE event_snapshots".command

  private val byteTable =
    sql"""CREATE TABLE event_snapshots2(
           exposure_id    VARCHAR(50) NOT NULL,
           obs_event_name VARCHAR(50) NOT NULL,
           source         VARCHAR(50) NOT NULL,
           eventName      VARCHAR(50) NOT NULL,
           eventId        VARCHAR(50) NOT NULL,
           eventTime      TIMESTAMP   NOT NULL,
           paramSet       bytea
         )""".command

  private val dropByteTable = sql"DROP TABLE event_snapshots2".command

  def withSnapshotTable(s: Session[IO]): Resource[IO, Unit] = {
    val alloc  = s.execute(createTable).void
    val free   = s.execute(dropTable).void
    val alloc2 = s.execute(byteTable).void
    val free2  = s.execute(dropByteTable).void
    Resource.make(alloc)(_ => free).flatMap(_ => Resource.make(alloc2)(_ => free2))
  }

  def write(expId: Int, s: MetadataService[IO]): IO[Unit] =
    for {
      times <- measure(s.insertSnapshot(SampleData.snapshot(expId)))
      _     <- IO(println(s"[Write] Time Taken = ${times._2}ms"))
    } yield ()

  def read(expId: Int, s: MetadataService[IO]): IO[Unit] =
    for {
      times <- measure(s.getSnapshot(expId.toString).compile.toList)
      _     <- IO(println(s"[Read] Rows = ${times._1.length}, Time Taken = ${times._2}ms"))
    } yield ()

  def write2(expId: Int, s: MetadataService[IO]): IO[Unit] =
    for {
      times <- measure(s.insertSnapshot2(SampleData.snapshot2(expId)))
      _     <- IO(println(s"[Write] Time Taken = ${times._2}ms"))
    } yield ()

  def read2(expId: Int, s: MetadataService[IO]): IO[Unit] =
    for {
      times <- measure(s.getSnapshot2(expId.toString).compile.toList)
      _     <- IO(println(s"[Read] Rows(*) = ${times._1.length}, Time Taken = ${times._2}ms"))
    } yield ()

  def read3(expId: Int, s: MetadataService[IO]): IO[Unit] =
    for {
      times <- measure(s.getParamSet(expId.toString).compile.toList)
      _     <- IO(println(s"[Read] Rows(paramSet) = ${times._1.length}, Time Taken = ${times._2}ms"))
    } yield ()

  def run(args: List[String]): IO[ExitCode] =
    session.flatTap(withSnapshotTable).flatMap(MetadataService.fromSession(_)).use { s =>
      for {
//        _ <- (1 to 20).map(write(_, s)).toList.sequence_
//        _ <- (1 to 20).map(read(_, s)).toList.sequence_
//        _ <- IO(println("=" * 100))
        _ <- (1 to 20).map(write2(_, s)).toList.sequence_
        _ <- (1 to 20).map(read2(_, s)).toList.sequence_
        _ <- (1 to 20).map(read3(_, s)).toList.sequence_
      } yield ExitCode.Success
    }

}

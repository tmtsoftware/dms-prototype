package metadata2

import cats.effect._
import natchez.Trace.Implicits.noop
import skunk._

object Main extends IOApp {
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "postgres",
      database = "postgres"
    )
  import cats.effect._
  import cats.syntax.all._

  import scala.concurrent.duration.MILLISECONDS

  def measure[F[_], A](fa: F[A])(implicit F: Sync[F], clock: Clock[F]): F[(A, Long)] = {

    for {
      start  <- clock.monotonic(MILLISECONDS)
      result <- fa
      finish <- clock.monotonic(MILLISECONDS)
    } yield (result, finish - start)
  }

  def run(args: List[String]): IO[ExitCode] =
    session.flatMap(MetadataService.fromSession(_)).use { s =>
      (for {
        times <- measure(s.insertSnapshot(SampleData.snapshotConst))
        _     <- IO(println(s"Time Taken = ${times._2}ms"))
      } yield ()).replicateA(50).map(_ => ExitCode.Success)
    }

}

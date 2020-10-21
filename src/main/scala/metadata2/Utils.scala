package metadata2

import cats.effect.{Clock, IO, Sync}
import cats.implicits._

import scala.concurrent.duration.MILLISECONDS

object Utils {

  def measure[F[_], A](fa: F[A])(implicit F: Sync[F], clock: Clock[F]): F[A] =
    for {
      start  <- clock.monotonic(MILLISECONDS)
      result <- fa
      finish <- clock.monotonic(MILLISECONDS)
      _      <- F.pure(println(s"Time Taken = ${finish - start}ms"))
    } yield result

  def pprint(msg: String): IO[Unit] =
    for {
      _ <- IO(println("=" * 60))
      _ <- IO(println(msg))
      _ <- IO(println("=" * 60))
    } yield ()
}

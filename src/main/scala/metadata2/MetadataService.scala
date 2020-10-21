package metadata2

import cats.Applicative
import cats.effect.{Bracket, _}
import cats.implicits._
import skunk._
import skunk.codec.all.varchar
import skunk.implicits._

trait MetadataService[F[_]] {
  def insertSnapshot(snapshot: List[SnapshotRow]): F[Unit]
  def getSnapshot(expId: String): fs2.Stream[F, SnapshotRow]
}

object MetadataService {
  import Codecs._

  private val selectSnapshotsByExpId: Query[String, SnapshotRow] =
    sql"SELECT * FROM event_snapshots WHERE exposure_id=$varchar".query(snapshotRow)

  private def insertMany(snapshot: List[SnapshotRow]): Command[snapshot.type] = {
    val enc = eventCodec.gcontramap[SnapshotRow].values.list(snapshot)
    sql"INSERT INTO event_snapshots VALUES $enc".command
  }

  def fromSession[F[_]: Applicative: Bracket[*[_], Throwable]](s: Session[F]): Resource[F, MetadataService[F]] =
    s.prepare(selectSnapshotsByExpId).map { pq =>
      new MetadataService[F] {
        override def insertSnapshot(snapshot: List[SnapshotRow]): F[Unit] =
          s.prepare(insertMany(snapshot)).use(_.execute(snapshot)).void

        override def getSnapshot(expId: String): fs2.Stream[F, SnapshotRow] = pq.stream(expId, 32)
      }
    }

}

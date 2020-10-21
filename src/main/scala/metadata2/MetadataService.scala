package metadata2

import cats.Applicative
import cats.effect.{Bracket, _}
import cats.implicits._
import cats.effect.implicits._
import cats.effect._
import csw.params.core.generics.Parameter
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait MetadataService[F[_]] {
  // paramSet = varchar
  def insertSnapshot(snapshot: List[SnapshotRow]): F[Unit]
  def getSnapshot(expId: String): fs2.Stream[F, SnapshotRow]

  // paramSet = bytea
  def insertSnapshot2(snapshot: List[SnapshotRow2]): F[Unit]
  def getSnapshot2(expId: String): fs2.Stream[F, SnapshotRow2]
  def getParamSet(expId: String): fs2.Stream[F, Set[Parameter[_]]]
}

object MetadataService {
  import Codecs._

  private val selectSnapshotsByExpId: Query[String, SnapshotRow] =
    sql"SELECT * FROM event_snapshots WHERE exposure_id=$varchar".query(snapshotRow)

  private def insertMany(snapshot: List[SnapshotRow]): Command[snapshot.type] = {
    val enc = eventCodec.gcontramap[SnapshotRow].values.list(snapshot)
    sql"INSERT INTO event_snapshots VALUES $enc".command
  }

  private val selectSnapshotsByExpId2: Query[String, SnapshotRow2] =
    sql"SELECT * FROM event_snapshots2 WHERE exposure_id=$varchar".query(snapshotRow2)

  private def insertMany2(snapshot: List[SnapshotRow2]): Command[snapshot.type] = {
    val enc = eventCodec2.gcontramap[SnapshotRow2].values.list(snapshot)
    sql"INSERT INTO event_snapshots2 VALUES $enc".command
  }

  private val paramSet: Query[String, Set[Parameter[_]]] =
    sql"SELECT paramSet FROM event_snapshots2 WHERE exposure_id = $varchar".query(paramSetDecoder)

  def fromSession[F[_]: Applicative: Bracket[*[_], Throwable]](s: Session[F]): Resource[F, MetadataService[F]] = {
    val pq1       = s.prepare(selectSnapshotsByExpId)
    val pq2       = s.prepare(selectSnapshotsByExpId2)
    val pq3       = s.prepare(paramSet)
    val resources = pq1.flatMap(p1 => pq2.flatMap(p2 => pq3.map(p3 => (p1, p2, p3))))

    resources.map {
      case (pq1, pq2, pq3) =>
        new MetadataService[F] {
          // paramSet = varchar
          override def insertSnapshot(snapshot: List[SnapshotRow]): F[Unit] =
            s.prepare(insertMany(snapshot)).use(_.execute(snapshot)).void

          override def getSnapshot(expId: String): fs2.Stream[F, SnapshotRow] = pq1.stream(expId, 32)

          // paramSet = bytea
          override def insertSnapshot2(snapshot: List[SnapshotRow2]): F[Unit] =
            s.prepare(insertMany2(snapshot)).use(_.execute(snapshot)).void

          override def getSnapshot2(expId: String): fs2.Stream[F, SnapshotRow2] = pq2.stream(expId, 32)

          override def getParamSet(expId: String): fs2.Stream[F, Set[Parameter[_]]] = pq3.stream(expId, 32)
        }
    }
  }

}

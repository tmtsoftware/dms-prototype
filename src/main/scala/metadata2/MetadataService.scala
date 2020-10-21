package metadata2

import cats.Applicative
import cats.effect.{Bracket, _}
import cats.implicits._
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
  private val eventSnapshots  = "event_snapshots"
  private val eventSnapshots2 = "event_snapshots2"

  private def selectByExpId[T](tableName: String, cols: String)(implicit decoder: Decoder[T]): Query[String, T] =
    sql"SELECT #$cols FROM #$tableName WHERE exposure_id = $varchar".query(decoder)

  private def insertMany[T](tableName: String, snapshot: List[T])(implicit codec: Encoder[T]): Command[snapshot.type] = {
    val enc = codec.values.list(snapshot)
    sql"INSERT INTO #$tableName VALUES $enc".command
  }

  def fromSession[F[_]: Applicative: Bracket[*[_], Throwable]](s: Session[F]): Resource[F, MetadataService[F]] = {
    val pq1       = s.prepare(selectByExpId(eventSnapshots, "*")(snapshotRow))
    val pq2       = s.prepare(selectByExpId(eventSnapshots2, "*")(snapshotRow2))
    val pq3       = s.prepare(selectByExpId(eventSnapshots2, "paramSet")(paramSetDecoder))
    val resources = pq1.flatMap(p1 => pq2.flatMap(p2 => pq3.map(p3 => (p1, p2, p3))))

    resources.map {
      case (pq1, pq2, pq3) =>
        new MetadataService[F] {

          // paramSet = varchar
          override def insertSnapshot(snapshot: List[SnapshotRow]): F[Unit] =
            s.prepare(insertMany(eventSnapshots, snapshot)(eventCodec.gcontramap[SnapshotRow])).use(_.execute(snapshot)).void
          override def getSnapshot(expId: String): fs2.Stream[F, SnapshotRow] = pq1.stream(expId, 32)

          // paramSet = bytea
          override def insertSnapshot2(snapshot: List[SnapshotRow2]): F[Unit] =
            s.prepare(insertMany(eventSnapshots + "2", snapshot)(eventCodec2.gcontramap[SnapshotRow2]))
              .use(_.execute(snapshot))
              .void

          override def getSnapshot2(expId: String): fs2.Stream[F, SnapshotRow2]     = pq2.stream(expId, 32)
          override def getParamSet(expId: String): fs2.Stream[F, Set[Parameter[_]]] = pq3.stream(expId, 32)
        }
    }
  }
}

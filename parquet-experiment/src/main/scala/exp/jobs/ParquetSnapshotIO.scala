package exp.jobs

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.github.mjakubowski84.parquet4s._
import exp.api.SystemEventRecord
import org.apache.parquet.hadoop.metadata.CompressionCodecName

import scala.concurrent.Future

class ParquetSnapshotIO(path: String)(implicit actorSystem: ActorSystem[_]) {

  def write(snapshot: Seq[SystemEventRecord]): Future[Done] = {
    Source(snapshot)
      .via(
        ParquetStreams
          .viaParquet[SystemEventRecord](path)
          .withWriteOptions(ParquetWriter.Options(compressionCodecName = CompressionCodecName.SNAPPY))
          .withPartitionBy("exposureId", "obsEventName")
          .build()
      )
      .run()
  }

  def read[P](
      exposureId: String
  )(implicit schemaResolver: ParquetSchemaResolver[P], decoder: ParquetRecordDecoder[P]): Future[Seq[P]] = {
    read(Col("exposureId") === exposureId)
  }

  def read[P](exposureId: String, obsEventName: String)(implicit
      schemaResolver: ParquetSchemaResolver[P],
      decoder: ParquetRecordDecoder[P]
  ): Future[Seq[P]] = {
    read(Col("exposureId") === exposureId && Col("obsEventName") === obsEventName)
  }

  def read[P](
      filter: Filter
  )(implicit schemaResolver: ParquetSchemaResolver[P], decoder: ParquetRecordDecoder[P]): Future[Seq[P]] = {
    ParquetStreams
      .fromParquet[P]
      .withProjection
      .withFilter(filter)
      .read(path)
      .runWith(Sink.seq)
  }
}

package exp.jobs

import java.io.File

import com.github.mjakubowski84.parquet4s.{ParquetReader, ParquetWriter}
import csw.EventFactory
import exp.api.{Constants, EventServiceMock, SystemEventRecord}
import org.apache.commons.io.FileUtils

import scala.concurrent.blocking

object ParquetBatchJob {
  val obsEventIds: Seq[String] = (1 to 10).map(x => s"obs-event-$x")

  def main(args: Array[String]): Unit = {

    val table = new File(Constants.BatchingDir)

    if (table.exists()) {
      FileUtils.deleteDirectory(table)
      println("deleted the existing table")
    }

    obsEventIds.foreach { obsEventId =>
      val start = System.currentTimeMillis()
      write(obsEventId, EventServiceMock.captureSnapshot(0, "startEvent"))
      val current = System.currentTimeMillis()
      println(s"Wrote the snapshot for observe-event:$obsEventId in ${current - start} millis >>>>>>>>>>>>>>>>")
    }

    obsEventIds.foreach { obsEventId =>
      val start = System.currentTimeMillis()
      read(obsEventId)
      val current = System.currentTimeMillis()
      println(s"Read the snapshot for observe-event:$obsEventId in ${current - start} millis <<<<<<<<<<<<<<<<<")
    }
  }

  def write(batchId: String, batch: Seq[SystemEventRecord]): Unit =
    blocking {
      ParquetWriter.writeAndClose(s"${Constants.BatchingDir}/$batchId.parquet", batch)
    }

  def read(batchId: String): Unit = {
    val parquetIterable = ParquetReader.read[SystemEventRecord](s"${Constants.BatchingDir}/$batchId.parquet")
    try {
      parquetIterable.map(EventFactory.fromRecord).foreach(_ => ())
    } finally parquetIterable.close()
  }

}

package exp.jobs.coversion

import java.io.File

import exp.api.SystemEventRecord2
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.{Encoders, SparkSession}

import scala.concurrent.duration.DurationInt

object P2DStreamingSwappingFileWatcherJob {
  def main(args: Array[String]): Unit = {

    FileUtils.deleteDirectory(new File("target/data"))
    val file = new File("target/data/temp")
//    val file = new File("target/data/parquet-streams")
    file.mkdirs()

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[*]")
//      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
//      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
//      .config("spark.delta.logStore.class", "org.apache.spark.sql.delta.storage.S3SingleDriverLogStore")
//      .config("spark.hadoop.fs.s3a.endpoint", "http://127.0.0.1:9000")
//      .config("spark.hadoop.fs.s3a.access.key", "minioadmin")
//      .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
//      .config("spark.hadoop.fs.s3a.path.style.access", "true")
//      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .getOrCreate()

    val dataFrame = spark.readStream
      .format("parquet")
      .schema(Encoders.product[SystemEventRecord2].schema)
      .load("target/data/temp")
//      .load("target/data/parquet-streams")
//      .load("hdfs://localhost:8020/target/data/parquet-streams")
//      .load("s3a://bucket1/target/data/delta-lake")
    dataFrame.printSchema()
    val query = dataFrame.writeStream
      .format("delta")
      .partitionBy("hour", "minute")
      .option("checkpointLocation", "target/data/cp/backup")
//      .option("checkpointLocation", "hdfs://localhost:8020/target/data/cp/backup")
//      .option("checkpointLocation", "s3a://bucket1/target/data/cp/backup")
      .trigger(Trigger.ProcessingTime(1.seconds))
      .start("target/data/delta-events-backup")
//      .start("hdfs://localhost:8020/target/data/delta-events-backup")
//      .start("s3a://bucket1/target/data/delta-events-backup")

    query.awaitTermination()
  }
}

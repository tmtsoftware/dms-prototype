package exp.jobs

import exp.api.SystemEventRecord
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.{Encoders, SparkSession}

import scala.concurrent.duration.DurationInt

object ParquetToDeltaEventsBatchJob {
  def main(args: Array[String]): Unit = {
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

    import spark.implicits._

    val dataFrame = spark.read
      .format("parquet")
      .schema(Encoders.product[SystemEventRecord].schema)
      .load("target/data/temp")
    //      .load("target/data/spark-sql")
    //      .load("hdfs://localhost:8020/target/data/parquet-streams")
    //      .load("s3a://bucket1/target/data/delta-lake")

    val query = dataFrame.write
      .format("delta")
      .partitionBy("exposureId", "obsEventName")
      .option("checkpointLocation", "target/data/cp/backup")
      .save("target/data/delta-events-backup-via-parq-stream")
    //      .option("checkpointLocation", "hdfs://localhost:8020/target/data/cp/backup")
    //      .option("checkpointLocation", "s3a://bucket1/target/data/cp/backup")
    //      .trigger(Trigger.ProcessingTime(1.seconds))
    //      .start("target/data/delta-events-backup-via-parq-stream")
    //      .start("hdfs://localhost:8020/target/data/delta-events-backup")
    //      .start("s3a://bucket1/target/data/delta-events-backup")

    //    query.awaitTermination()
  }
}

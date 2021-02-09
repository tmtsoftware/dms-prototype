package exp.jobs.coversion

import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DataType, StructType}

object ConversionJob {
  def main(args: Array[String]): Unit = {

    FileUtils.deleteDirectory(new File("target/data"))
    new File("target/data/json").mkdirs()

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[4]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    val schema = DataType.fromJson(Files.readString(Paths.get("delta-writer/src/main/resources/schema.json")))
    import org.apache.spark.sql.functions._
    import spark.implicits._

    val dataFrame = spark.readStream
      .format("json")
      .schema(schema.asInstanceOf[StructType])
      .option("cleanSource", "delete")
      .load("target/data/json")
      .select(
        $"*",
        year($"eventTime").alias("year"),
        month($"eventTime").alias("month"),
        dayofmonth($"eventTime").alias("day"),
        hour($"eventTime").alias("hour")
      )

    val query = dataFrame.writeStream
      .format("delta")
      .partitionBy("year", "month", "day", "hour", "source", "eventName")
      .option("checkpointLocation", "target/data/cp/backup")
      //      .trigger(Trigger.ProcessingTime(1.seconds))
      .start("target/data/delta")
//      .start("hdfs://localhost:8020/data/delta")

    query.awaitTermination()
  }
}

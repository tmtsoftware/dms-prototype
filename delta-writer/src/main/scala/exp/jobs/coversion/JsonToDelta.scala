package exp.jobs.coversion

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DataType, StructType}

import java.io.File
import java.nio.file.{Files, Paths}

object JsonToDelta {
  def main(args: Array[String]): Unit = {

    FileUtils.deleteDirectory(new File("target/data"))
    new File("target/data/json").mkdirs()

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[1]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    val schema = DataType.fromJson(Files.readString(Paths.get("delta-writer/src/main/resources/schema.json")))
    import org.apache.spark.sql.functions._
    import spark.implicits._

    val dataFrame = spark.readStream
      .format("json")
      .schema(schema.asInstanceOf[StructType])
      .load("target/data/json")
      .select(
        $"*",
        year($"eventTime").alias("year"),
        month($"eventTime").alias("month"),
        dayofmonth($"eventTime").alias("day")
      )

    val query = dataFrame.writeStream
      .format("delta")
      .partitionBy("year", "month", "day", "source", "eventName")
      .option("checkpointLocation", "target/data/cp/backup")
      //      .trigger(Trigger.ProcessingTime(1.seconds))
      .start("target/data/delta")

    query.awaitTermination()
  }
}

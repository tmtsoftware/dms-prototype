package eng.arch.conversion.job

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types.{DataType, StructType}

import java.nio.file.{Files, Paths}
import scala.concurrent.duration.DurationInt
object ConversionJob {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[1]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.driver.memory", "4g")
      .getOrCreate()

    val schema = DataType.fromJson(Files.readString(Paths.get("eng-arch/eng-arch-conversion-job/src/main/resources/schema.json")))
//    val path   = getClass.getResource("/schema.json").getPath //todo: below two line works with sbt but not with coursier
//    val schema = DataType.fromJson(Files.readString(Paths.get(path)))

    import org.apache.spark.sql.functions._
    import spark.implicits._

    val dataFrame = spark.readStream
      .format("json")
      .schema(schema.asInstanceOf[StructType])
      .option("cleanSource", "delete")
      .load("target/data/json")
      .select(
        $"*",
        to_timestamp($"eventTime").as("eventTimeStamp"),
        unix_timestamp(to_timestamp($"eventTime")).as("eventUnixTimeStamp"),
        year($"eventTime").as("year"),
        month($"eventTime").as("month"),
        dayofmonth($"eventTime").as("day"),
        hour($"eventTime").as("hour"),
        minute($"eventTime").as("minute"),
        second($"eventTime").as("second")
      )
      .repartition($"year", $"month", $"day", $"source", $"eventName")

    val query = dataFrame.writeStream
      .format("delta")
      .partitionBy("year", "month", "day", "source", "eventName")
      .option("checkpointLocation", "target/data/cp")
      .trigger(Trigger.ProcessingTime(5.minutes))
      .start("target/data/delta")

    query.awaitTermination()
  }
}

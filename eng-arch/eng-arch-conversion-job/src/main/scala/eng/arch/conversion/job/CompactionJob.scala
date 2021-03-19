package eng.arch.conversion.job

import org.apache.spark.sql.SparkSession

object CompactionJob extends App {

  val spark = SparkSession
    .builder()
    .appName(getClass.getSimpleName)
    .master("local[1]")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    .config("spark.driver.memory", "6g")
    .getOrCreate()

  import spark.implicits._

  val path = "target/data/delta"

  spark.read
    .format("delta")
    .load(path)
    .repartition($"year", $"month", $"day", $"source", $"eventName")
    .sortWithinPartitions("year", "month", "day", "source", "eventName", "eventTimeStamp")
    .write
    .format("delta")
    .partitionBy("year", "month", "day", "source", "eventName")
    .option("dataChange", "false")
    .mode("append")
    .save(path)
}

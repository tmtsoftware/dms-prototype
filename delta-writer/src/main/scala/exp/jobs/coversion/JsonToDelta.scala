package exp.jobs.coversion

import java.io.File

import exp.api.SystemEventRecord
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.{Encoders, SparkSession}

object JsonToDelta {
  def main(args: Array[String]): Unit = {

    FileUtils.deleteDirectory(new File("target/data"))
    val file = new File("target/data/json")
    file.mkdirs()

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[1]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    val dataFrame = spark.readStream
      .format("json")
      .schema(Encoders.product[SystemEventRecord].schema)
      .load("target/data/json")

    val query = dataFrame.writeStream
      .format("delta")
      .partitionBy("date", "hour", "minute")
      .option("checkpointLocation", "target/data/cp/backup")
      //      .trigger(Trigger.ProcessingTime(1.seconds))
      .start("target/data/delta")

    query.awaitTermination()
  }
}

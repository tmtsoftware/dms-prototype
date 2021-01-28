package exp.jobs.coversion

import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object InferJsonSchema {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[*]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    val df = spark.read
      .format("json")
      .option("multiLine", value = true)
      .load("target/data/json")

    Files.writeString(Paths.get("target/data/schema.json"), df.schema.prettyJson)

    df.printSchema()
  }
}

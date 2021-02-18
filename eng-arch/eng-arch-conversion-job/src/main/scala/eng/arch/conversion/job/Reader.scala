package eng.arch.conversion.job

import org.apache.spark.sql.SparkSession

object Reader extends App {

  val spark = SparkSession
    .builder()
    .appName(getClass.getSimpleName)
    .master("local[1]")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    .getOrCreate()

  import org.apache.spark.sql.functions._
  import spark.implicits._

  val dataFrame = spark.read.format("delta").load("target/data/delta")

  dataFrame
    .filter($"year" === 2021 && $"month" === 2 && $"day" === 25 && $"hour" === 23)
    .filter("eventName=='event_key' and source=='ESW.filter'")

//    .filter(array_contains($"paramSet.IntKey.keyName", "IntKey123"))
    //    .withColumn("abc", explode($"paramSet.IntKey.values"))
    ////    .select($"abc" (0))
    //    .show
    .withColumn("parameter", explode($"paramSet.IntKey"))
    .filter($"parameter.keyName" === "IntKey123")
    .select($"eventTime", $"parameter.values" (0) as "values")
    .show

  // column selection
//    .withColumn(
//      "strKey123",
//      explode(filter($"paramSet", x => x("StringKey")("keyName") === "StringKey123")("StringKey")("values")(0))
//    )
//    .withColumn(
//      "intKey456",
//      explode(filter($"paramSet", x => x("IntKey")("keyName") === "IntKey456")("IntKey")("values")(0))
//    )
//    .show

}

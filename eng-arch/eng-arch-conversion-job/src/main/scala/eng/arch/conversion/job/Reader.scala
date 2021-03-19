package eng.arch.conversion.job

import org.apache.spark.sql.SparkSession

object Reader extends App {

  val spark = SparkSession
    .builder()
    .appName(getClass.getSimpleName)
    .master("local[1]")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    .config("spark.driver.memory", "4g")
    .getOrCreate()

  import org.apache.spark.sql.functions._
  import spark.implicits._

  val df = spark.read.format("delta").load("target/data/delta")

  private def df_time =
    df.filter("year==2021 and month==3 and day==19")
      .filter($"eventTime" >= "2021-03-19T07:08:39.066141Z" && $"eventTime" <= "2021-03-19T07:08:40.066141Z")

  private def df_result1 =
    df_time
      .filter("source=='Container.filter'")
      .filter("eventName == 'event_key_1'")
      .withColumn("temperature", filter($"paramSet.IntKey", x => x("keyName") === "temperature")) //&& x("values")(0) > 9)(0)
      .select($"eventTime", $"temperature.values" (0)(0) as "temperature")

  private def df_result2 =
    df_time
      .filter("source=='Container.filter'")
      .filter("eventName == 'event_key_2'")
      .withColumn("intensity", filter($"paramSet.IntKey", x => x("keyName") === "intensity"))
      .select($"eventTime", $"intensity.values" (0)(0) as "intensity")

  def singleEventQuery = df_result1

  def multipleEventJoinQuery =
    df_result1
      .join(df_result2, Seq("eventTime"), "fullOuter")
      .orderBy("eventTime")

  (1 to 100).foreach { _ => println("Rows : " + spark.time(singleEventQuery.collect().length)) }
}

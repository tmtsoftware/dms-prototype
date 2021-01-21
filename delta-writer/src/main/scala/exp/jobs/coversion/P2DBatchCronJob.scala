package exp.jobs.coversion

import java.time.{Instant, LocalDateTime, ZoneOffset}

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import org.apache.spark.sql.SparkSession

import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

object P2DBatchCronJob {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .master("local[1]") //
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.delta.logStore.class", "org.apache.spark.sql.delta.storage.S3SingleDriverLogStore")
      .config("spark.hadoop.fs.s3a.endpoint", "http://127.0.0.1:9000")
      .config("spark.hadoop.fs.s3a.access.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .getOrCreate()

    def transfer(date: String, hour: Int, minute: Int) = {
      val dataFrame = spark.read.format("parquet").load(s"target/data/parquet-streams")

      dataFrame
        .filter(s"date == '$date' and hour == $hour and minute == $minute")
        .write
        .mode(org.apache.spark.sql.SaveMode.Append)
        .partitionBy("date", "hour")
        .save("target/data/delta-events-backup-vai-batch")

      println(s"saved for => $date, $hour, $minute")
    }

    val currentTime    = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
    val currentSeconds = currentTime.getSecond
    val initialDelay   = FiniteDuration(70 - currentSeconds, SECONDS) // to schedule on * minute 10th second.

    implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem.apply(SpawnProtocol(), "transformer")

    Source
      .tick(initialDelay, 1.minute, ())
      .map { _ =>
        val previousMinute = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusMinutes(1)
        transfer(previousMinute.toLocalDate.toString, previousMinute.getHour, previousMinute.getMinute)
      }
      .run()
  }

}

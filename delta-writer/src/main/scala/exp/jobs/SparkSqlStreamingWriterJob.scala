package exp.jobs

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import exp.api.SparkTable
import org.apache.spark.sql.SparkSession

import scala.util.{Failure, Success}

object SparkSqlStreamingWriterJob {
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "demo")

    val spark = SparkSession
      .builder()
      .appName(getClass.getSimpleName)
      .config("spark.hadoop.fs.s3a.endpoint", "http://127.0.0.1:9000")
      .config("spark.hadoop.fs.s3a.access.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .master("local[*]")
      .getOrCreate()

//    val streamingWriter = new StreamingWriter(new SparkTable(spark, "target/data/spark-sql", "parquet"))
    val streamingWriter = new StreamingWriter(new SparkTable(spark, "s3a://bucket1/target/data/spark-sql", "parquet"))

    import actorSystem.executionContext

    streamingWriter.run().onComplete { x =>
      spark.stop()
      actorSystem.terminate()
      x match {
        case Failure(exception) => exception.printStackTrace()
        case Success(value)     =>
      }
    }
  }
}

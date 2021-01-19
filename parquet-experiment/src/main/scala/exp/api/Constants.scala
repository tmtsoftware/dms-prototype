package exp.api

object Constants {
  val StreamingDir     = "target/data/parquet-streams"
  val StreamingHdfsDir = s"hdfs://localhost:8020/${Constants.StreamingDir}"
  val StreamingS3Dir   = s"s3a://bucket3/${Constants.StreamingDir}"
  val BatchingDir      = "target/data/parquet-batches"
}

// to use this script run the following command from the project root
// cs launch ammonite --scala 2.12.13 -- --class-based --predef-code 'import $exec.sparkShell'
import $ivy.`org.apache.spark::spark-sql:3.0.1`
import $ivy.`sh.almond::ammonite-spark:0.11.0`
import $ivy.`io.delta::delta-core:0.8.0`
import org.apache.spark.sql._
val spark = {
  AmmoniteSparkSession.builder()
    .master("local[*]")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    .getOrCreate()
}

val sc = spark.sparkContext
sc.setLogLevel("ERROR")

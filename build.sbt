name := "dms-prototype"

version := "0.1"

scalaVersion := "2.13.3"

resolvers += "jitpack" at "https://jitpack.io"

val cswVersion = "4a67b51"

libraryDependencies ++= Seq(
  "com.github.tmtsoftware.csw" %% "csw-location-client" % cswVersion,
  "com.github.tmtsoftware.csw" %% "csw-event-client"    % cswVersion,
  "com.github.tmtsoftware.csw" %% "csw-database"        % cswVersion,
  "org.hdrhistogram"            % "HdrHistogram"        % "2.1.12",
  "gov.nasa.gsfc.heasarc"       % "nom-tam-fits"        % "1.15.2",
  "com.jayway.jsonpath"         % "json-path"           % "2.4.0"
)

lazy val `dms-spark` = project
  .settings(
    scalaVersion := "2.12.12",
    libraryDependencies ++= Seq(
      "io.delta"          %% "delta-core"        % "0.7.0",
      "org.apache.spark"  %% "spark-sql"         % "3.0.1",
      "com.typesafe.akka" %% "akka-stream-typed" % "2.6.10",
      "io.bullet"         %% "borer-derivation"  % "1.6.2",
      "org.apache.hadoop"  % "hadoop-aws"        % "2.7.4",
      "org.postgresql"     % "postgresql"        % "42.2.16"
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint:-unused,_",
      "-Ywarn-dead-code",
      "-Xfuture"
    )
  )

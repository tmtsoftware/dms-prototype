import Common._

inThisBuild(
  CommonSettings
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

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `dms-metadata-access`,
  `dms-metadata-collection`
)

lazy val root = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

lazy val `dms-metadata-collection` = project
  .in(file("dms-metadata-collection"))
  .settings(
    libraryDependencies ++= Dependencies.MetadataCollection.value
  )

lazy val `dms-metadata-access` = project
  .in(file("dms-metadata-access"))
  .aggregate(`dms-metadata-access-api`, `dms-metadata-access-impl`)

lazy val `dms-metadata-access-api` = project
  .in(file("dms-metadata-access/dms-metadata-access-api"))

lazy val `dms-metadata-access-impl` = project
  .in(file("dms-metadata-access/dms-metadata-access-impl"))
  .settings(
    libraryDependencies ++= Dependencies.MetadataAccessImpl.value
  )
  .dependsOn(`dms-metadata-access-api`)

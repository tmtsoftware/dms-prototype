lazy val commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := "2.13.3",
  resolvers += "jitpack" at "https://jitpack.io"
)

inThisBuild(
  commonSettings
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
  `dms-metadata-access-api`,
  `dms-metadata-access-impl`,
  `dms-metadata-collection`
)

lazy val root = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)
  .settings(
    name := "dms-prototype"
  )

lazy val `dms-metadata-collection` = project
  .settings(
    libraryDependencies ++= Dependencies.MetadataCollection.value
  )

lazy val `dms-metadata-access` = project
  .aggregate(`dms-metadata-access-api`, `dms-metadata-access-impl`)

lazy val `dms-metadata-access-api` = project
  .in(file("dms-metadata-access/dms-metadata-access-api"))
  .settings(commonSettings)

lazy val `dms-metadata-access-impl` = project
  .in(file("dms-metadata-access/dms-metadata-access-impl"))
  .settings(
    libraryDependencies ++= Dependencies.MetadataAccessImpl.value
  )
  .dependsOn(`dms-metadata-access-api`)

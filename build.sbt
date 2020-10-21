name := "dms-prototype"

version := "0.1"

scalaVersion := "2.13.3"

resolvers += "jitpack" at "https://jitpack.io"

val cswVersion = "098d6fc"
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "com.github.tmtsoftware.csw" %% "csw-location-client" % cswVersion,
  "com.github.tmtsoftware.csw" %% "csw-event-client"    % cswVersion,
  "com.github.tmtsoftware.csw" %% "csw-database"        % cswVersion,
  "org.tpolecat"               %% "skunk-core"          % "0.0.20",
  "org.hdrhistogram"            % "HdrHistogram"        % "2.1.12"
)

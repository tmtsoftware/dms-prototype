name := "dms-prototype"

version := "0.1"

scalaVersion := "2.13.3"

resolvers += "jitpack" at "https://jitpack.io"

val cswVersion = "098d6fc"

libraryDependencies ++= Seq(
  "com.github.tmtsoftware.csw" %% "csw-location-client" % cswVersion,
  "com.github.tmtsoftware.csw" %% "csw-event-client"    % cswVersion,
  "com.github.tmtsoftware.csw" %% "csw-database"        % cswVersion,
  "org.hdrhistogram"            % "HdrHistogram"        % "2.1.12"
)

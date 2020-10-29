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

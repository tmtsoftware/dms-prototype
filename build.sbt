name := "dms-prototype"

version := "0.1"

scalaVersion := "2.13.3"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
 "com.github.tmtsoftware.csw" %% "csw-location-client" % "620786ec2b",
 "com.github.tmtsoftware.csw" %% "csw-event-client" % "620786ec2b",
 "org.hdrhistogram"         % "HdrHistogram"      % "2.1.12"
)
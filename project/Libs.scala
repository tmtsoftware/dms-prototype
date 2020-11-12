import sbt._

object Csw {
  private val Org           = "com.github.tmtsoftware.csw"
  private val Version       = "4a67b51"
  val `csw-location-client` = Org %% "csw-location-client" % Version
  val `csw-event-client`    = Org %% "csw-event-client"    % Version
  val `csw-database`        = Org %% "csw-database"        % Version
}

object Libs {
  val `nom-tam-fits` = "gov.nasa.gsfc.heasarc" % "nom-tam-fits" % "1.15.2"
  val `HdrHistogram` = "org.hdrhistogram"      % "HdrHistogram" % "2.1.12"
}

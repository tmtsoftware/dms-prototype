import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt._
import sbt.librarymanagement.ScmInfo

object Common {
  private val enableFatalWarnings: Boolean = sys.props.get("enableFatalWarnings").contains("true")

  lazy val CommonSettings: Seq[Setting[_]] =
    Seq(
      organization := "com.github.tmtsoftware.dms-prototype",
      organizationName := "TMT Org",
      scalaVersion := DmsKeys.scalaVersion,
      scmInfo := Some(ScmInfo(url(DmsKeys.homepageValue), "git@github.com:tmtsoftware/dms-prototype.git")),
      resolvers += "jitpack" at "https://jitpack.io",
      autoCompilerPlugins := true,
      fork:=true,
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-feature",
        "-unchecked",
        "-deprecation",
        //-W Options
//        "-Wdead-code", //  fixme
//        if (enableFatalWarnings) "-Wconf:any:error" else "-Wconf:any:warning-verbose",
        //-X Options
        "-Xlint:_,-missing-interpolator",
        "-Xsource:3",
        "-Xcheckinit",
        "-Xasync"
        // -Y options are rarely needed, please look for -W equivalents
      ),
      licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
      publishArtifact in (Test, packageBin) := true,
      version := {
        sys.props.get("prod.publish") match {
          case Some("true") => version.value
          case _            => "0.1.0-SNAPSHOT"
        }
      },
      isSnapshot := !sys.props.get("prod.publish").contains("true"),
      cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
      scalafmtOnCompile := true,
      Global / excludeLintKeys := Set(
        SettingKey[Boolean]("ide-skip-project"),
        aggregate //verify if this needs to be here or our configuration is wrong
      )
    )
}

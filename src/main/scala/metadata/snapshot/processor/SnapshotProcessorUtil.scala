package metadata.snapshot.processor

import java.io.{ByteArrayOutputStream, File, PrintStream}

import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import metadata.snapshot.processor.HeaderConfig.{ComplexConfig, SimpleConfig}
import nom.tam.fits.Header

import scala.jdk.CollectionConverters.CollectionHasAsScala

object SnapshotProcessorUtil {

  def generateFormattedHeader(keywords: Seq[String], headerData: Map[String, Option[String]]) = {
    val fitsHeader = new Header()
    keywords.foreach { keyword =>
      headerData.get(keyword).flatten.map { fitsHeader.addValue(keyword, _, "") }
    }

    val os = new ByteArrayOutputStream();
    val ps = new PrintStream(os);

    fitsHeader.dumpHeader(ps)

    val output = os.toString("UTF8")
    output
  }

  def loadHeaderConfig(): Map[Subsystem, List[HeaderConfig]] = {
    val subsystems: List[Subsystem] = List(IRIS, WFOS)
    subsystems.map { subsystem =>
      val baseConfigPath           = getClass.getResource("/base-keyword-mappings.conf").getPath
      val instrumentConfigPath     = getClass.getResource(s"/${subsystem.name}-keyword-mappings.conf").getPath
      val baseConfig: Config       = ConfigFactory.parseFile(new File(baseConfigPath))
      val instrumentConfig: Config = ConfigFactory.parseFile(new File(instrumentConfigPath)).withFallback(baseConfig).resolve()
      val keywords                 = instrumentConfig.root().keySet().asScala.toList
      val headerConfigList = keywords.map { keyword =>
        val complexConfig: Config = instrumentConfig.getConfig(keyword)
        if (complexConfig.hasPath("value")) {
          SimpleConfig(keyword, complexConfig.getString("value"))
        }
        else {
          val obsEventName = complexConfig.getString("obs-event-name")
          val eventKey     = complexConfig.getString("event-key")
          val paramKey     = complexConfig.getString("param-key")
          val jsonPath     = complexConfig.getString("field-path")
          ComplexConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
        }
      }
      subsystem -> headerConfigList
    }.toMap
  }

  def loadHeaderList(): Map[Subsystem, List[String]] = {
    val path           = getClass.getResource("/header-keywords.conf").getPath
    val config: Config = ConfigFactory.parseFile(new File(path))
    val subsystemNames = config.root().keySet().asScala.toList
    subsystemNames.map { subsystemName =>
      val maybeSubsystem: Option[Subsystem] = Subsystem.values.toList.find(_.name == subsystemName)
      maybeSubsystem match {
        case Some(_) => maybeSubsystem.get -> config.getStringList(subsystemName).asScala.toList
        case None    => throw new RuntimeException(s"Invalid Subsystem Name : $subsystemName")
      }
    }.toMap
  }
}

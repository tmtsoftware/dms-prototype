package metadata.snapshot.processor

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.events._
import csw.prefix.models.Subsystem
import metadata.snapshot.processor.HeaderConfig.{ComplexConfig, SimpleConfig}
import nom.tam.fits.Header

import scala.jdk.CollectionConverters.{CollectionHasAsScala, ConcurrentMapHasAsScala}

sealed trait HeaderConfig
object HeaderConfig {
  final case class ComplexConfig(keyword: String, obsEventName: String, eventKey: String, paramKey: String, jsonPath: String)
      extends HeaderConfig
  final case class SimpleConfig(keyword: String, value: String) extends HeaderConfig
}
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

  def getHeaderData(
      snapshotObsEventName: String,
      snapshot: ConcurrentHashMap[EventKey, Event]
  ): List[(String, Option[String])] = {
    val headerConfigs: List[HeaderConfig] = loadHeaderConfig()
    val headerData: List[(String, Option[String])] = headerConfigs
      .filter {
        case ComplexConfig(_, configObsEventName, _, _, _) if snapshotObsEventName == configObsEventName => true
        case SimpleConfig(_, _) if snapshotObsEventName == "exposureStart"                               => true
        case _                                                                                           => false
      }
      .map {
        case ComplexConfig(keyword, _, eventKey, paramKey, jsonPath) =>
          val value = snapshot.asScala
            .get(EventKey(eventKey))
            .map(e => e.paramSet)
            .flatMap(_.find(p => p.keyName.equals(paramKey)))
            .map(_.head match { case s: String => s })
          (keyword, value)
        case SimpleConfig(keyword, value) => (keyword, Some(value))
      }
    headerData
  }

  def loadHeaderConfig(): List[HeaderConfig] = {
    val baseConfigPath           = getClass.getResource("/base-header-mappings.conf").getPath
    val instrumentConfigPath     = getClass.getResource(s"/IRIS-header-mappings.conf").getPath
    val baseConfig: Config       = ConfigFactory.parseFile(new File(baseConfigPath))
    val instrumentConfig: Config = ConfigFactory.parseFile(new File(instrumentConfigPath)).withFallback(baseConfig).resolve()
    val keywords                 = instrumentConfig.root().keySet().asScala.toList
    keywords.map { keyword =>
      val keywordConfig: Config = instrumentConfig.getConfig(keyword)
      if (keywordConfig.hasPath("value")) {
        SimpleConfig(keyword, keywordConfig.getString("value"))
      }
      else {
        val obsEventName = keywordConfig.getString("obs-event-name")
        val eventKey     = keywordConfig.getString("event-key")
        val paramKey     = keywordConfig.getString("param-key")
        val jsonPath     = keywordConfig.getString("json-path")
        ComplexConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
      }
    }
  }

  def loadHeaderList(): Map[Subsystem, List[String]] = {
    val path           = getClass.getResource("/header-list.conf").getPath
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

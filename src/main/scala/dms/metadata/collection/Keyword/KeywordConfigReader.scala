package dms.metadata.collection.Keyword

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import dms.metadata.collection.Keyword.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class KeywordConfigReader {

  val subsystems: List[Subsystem]                             = List(IRIS, WFOS) // FIXME Should this be passed as config/ or consider all subsystem
  lazy val keywordValueExtractor: KeywordValueExtractor       = new KeywordValueExtractor
  lazy val headerConfigs: Map[Subsystem, List[KeywordConfig]] = load(subsystems)

  def extractKeywordValuesFor(
      configSubsystem: Subsystem,
      obsEventToProcess: Event,
      snapshot: ConcurrentHashMap[EventKey, Event]
  ): Map[String, String] = {
    val headersValues: Map[String, String] = headerConfigs(configSubsystem)
      .filter(_.obsEventName == obsEventToProcess.eventName.name)
      .map {
        case x: ComplexKeywordConfig               => (x.keyword, keywordValueExtractor.extract(x, snapshot))
        case ConstantKeywordConfig(keyword, value) => (keyword, value)
      }
      .toMap
    headersValues
  }

  private def load(subsystems: List[Subsystem]): Map[Subsystem, List[KeywordConfig]] = {
    subsystems.map { subsystem =>
      val baseConfigPath           = getClass.getResource("/base-keyword-mappings.conf").getPath
      val instrumentConfigPath     = getClass.getResource(s"/${subsystem.name}-keyword-mappings.conf").getPath
      val baseConfig: Config       = ConfigFactory.parseFile(new File(baseConfigPath))
      val instrumentConfig: Config = ConfigFactory.parseFile(new File(instrumentConfigPath)).withFallback(baseConfig).resolve()
      val keywords                 = instrumentConfig.root().keySet().asScala.toList
      val headerConfigList = keywords.map { keyword =>
        val complexConfig: Config = instrumentConfig.getConfig(keyword)
        if (complexConfig.hasPath("value")) {
          ConstantKeywordConfig(keyword, complexConfig.getString("value"))
        }
        else {
          val obsEventName = complexConfig.getString("obs-event-name")
          val eventKey     = complexConfig.getString("event-key")
          val paramKey     = complexConfig.getString("param-key")
          val jsonPath     = complexConfig.getString("field-path")
          ComplexKeywordConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
        }
      }
      subsystem -> headerConfigList
    }.toMap
  }

}

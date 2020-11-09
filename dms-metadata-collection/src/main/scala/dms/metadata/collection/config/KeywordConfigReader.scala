package dms.metadata.collection.config

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import dms.metadata.collection.config.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}

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
        case ConstantKeywordConfig(keyword, value) => (keyword, Some(value))
      }
      .collect { case (keyword, Some(value)) => keyword -> value }
      .toMap
    headersValues
  }

  private def load(subsystems: List[Subsystem]): Map[Subsystem, List[KeywordConfig]] = {
    subsystems.map { subsystem =>
      val baseConfig       = ConfigFactory.parseResources("base-keyword-mappings.conf")
      val instrumentConfig = ConfigFactory.parseResources(s"${subsystem.name}-keyword-mappings.conf").withFallback(baseConfig)
      val keywords         = instrumentConfig.root().keySet().asScala.toList
      val headerConfigList = keywords.map { keyword =>
        val complexConfig: Config = instrumentConfig.getConfig(keyword)
        if (complexConfig.hasPath("value")) {
          ConstantKeywordConfig(keyword, complexConfig.getString("value"))
        }
        else {
          val obsEventName = complexConfig.getString("obs-event-name")
          val eventKey     = complexConfig.getString("event-key")
          val paramKey     = complexConfig.getString("param-key")
          val attribute    = complexConfig.getString("attribute")
          ComplexKeywordConfig(keyword, obsEventName, eventKey, paramKey, attribute)
        }
      }
      subsystem -> headerConfigList
    }.toMap
  }

}

package dms.metadata.collection.config

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.ConfigFactory
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import dms.metadata.collection.config.KeywordConfig.{ComplexKeywordConfig, SimpleKeywordConfig}

object KeywordConfigReader extends App {
  (new KeywordConfigReader).headerConfigs.foreach(println)
}

class KeywordConfigReader {

  val subsystems: List[Subsystem]                                    = List(IRIS, WFOS) // FIXME Should this be passed as config/ or consider all subsystem
  lazy val keywordValueExtractor: KeywordValueExtractor              = new KeywordValueExtractor
  lazy val headerConfigs: Map[Subsystem, Map[String, KeywordConfig]] = load(subsystems)

  def extractKeywordValuesFor(
      configSubsystem: Subsystem,
      obsEventToProcess: Event,
      snapshot: ConcurrentHashMap[EventKey, Event]
  ): Map[String, String] = {
    val headersValues: Map[String, String] = headerConfigs(configSubsystem)
      .filter {
        case (_, config) => config.obsEventName == obsEventToProcess.eventName.name
      }
      .map {
        case (keyword, config: ComplexKeywordConfig) => (keyword, keywordValueExtractor.extract(config, snapshot))
        case (keyword, SimpleKeywordConfig(value))   => (keyword, Some(value))
      }
      .collect { case (keyword, Some(value)) => keyword -> value }
    headersValues
  }

  private def load(subsystems: List[Subsystem]): Map[Subsystem, Map[String, KeywordConfig]] = {
    subsystems.map { subsystem =>
      val baseConfig = ConfigFactory.parseResources("base-keyword-mappings.conf")
      val instrumentConfig =
        ConfigFactory.parseResources(s"${subsystem.name}-keyword-mappings.conf").withFallback(baseConfig).resolve()
      subsystem -> KeywordConfig.from(instrumentConfig)
    }.toMap
  }

}

package dms.metadata.collection.config

import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import dms.metadata.collection.config.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

class KeywordConfigReader {
  private val subsystems: List[Subsystem]                     = List(IRIS, WFOS) // FIXME Should this be passed as config/ or consider all subsystem
  lazy val headerConfigs: Map[Subsystem, List[KeywordConfig]] = load(subsystems)

  private def load(subsystems: List[Subsystem]): Map[Subsystem, List[KeywordConfig]] = {
    subsystems.map { subsystem =>
      val baseConfig       = ConfigFactory.parseResources("base-keyword-mappings.conf")
      val instrumentConfig = ConfigFactory.parseResources(s"${subsystem.name}-keyword-mappings.conf").withFallback(baseConfig)
      val keywords         = instrumentConfig.root().keySet().asScala.toList
      val headerConfigList = keywords.map { keyword =>
        val keywordConfig = instrumentConfig.getConfig(keyword)
        if (isConstantConfig(keywordConfig)) readConstantConfig(keyword, keywordConfig)
        else readComplexConfig(keyword, keywordConfig)
      }
      subsystem -> headerConfigList
    }.toMap
  }

  private def isConstantConfig(config: Config) = config.hasPath("value")

  private def readConstantConfig(keyword: String, constantConfig: Config) = {
    val value = constantConfig.getString("value")
    ConstantKeywordConfig(keyword, value)
  }

  private def readComplexConfig(keyword: String, complexConfig: Config) = {
    val obsEventName = complexConfig.getString("obs-event-name")
    val eventKey     = complexConfig.getString("event-key")
    val paramKey     = complexConfig.getString("param-key")
    val attribute    = Try(complexConfig.getString("attribute")).toOption
    ComplexKeywordConfig(keyword, obsEventName, eventKey, paramKey, attribute)
  }

}

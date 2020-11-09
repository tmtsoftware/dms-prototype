package dms.metadata.collection.config

import com.typesafe.config.Config
import pureconfig.generic.semiauto._
import pureconfig.{ConfigReader, ConfigSource}

sealed trait KeywordConfig {
  def obsEventName: String
}

object KeywordConfig {
  case class SimpleKeywordConfig(value: String) extends KeywordConfig {
    val obsEventName = "exposureStart" //FIXME read value from config or find better approach
  }

  case class ComplexKeywordConfig(obsEventName: String, eventKey: String, paramKey: String, attribute: Option[String])
      extends KeywordConfig {
    val paramPath: List[ParamPath] = ParamPath.from(paramKey)
  }

  implicit val simpleKeywordConfigReader: ConfigReader[SimpleKeywordConfig]   = deriveReader[SimpleKeywordConfig]
  implicit val complexKeywordConfigReader: ConfigReader[ComplexKeywordConfig] = deriveReader[ComplexKeywordConfig]
  private implicit val keywordConfigReader: ConfigReader[KeywordConfig] =
    ConfigReader[ComplexKeywordConfig].orElse(ConfigReader[SimpleKeywordConfig])

  def from(config: Config): Map[String, KeywordConfig] = ConfigSource.fromConfig(config).loadOrThrow[Map[String, KeywordConfig]]
}

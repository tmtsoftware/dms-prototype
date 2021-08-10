package dms.metadata.collection.config

import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import dms.metadata.collection.config.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}

import java.nio.file.Path
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

class KeywordConfigReader(externalConfigPath: List[Path]) {
  lazy val subsystems: List[Subsystem]                        = List(IRIS, WFOS) // FIXME Should this be passed as config/ or consider all subsystem
  lazy val headerConfigs: Map[Subsystem, List[KeywordConfig]] = load()

  private def load(): Map[Subsystem, List[KeywordConfig]] = {
    if (externalConfigPath.nonEmpty) loadFromResources() ++ loadFromExternalConfig()
    else loadFromResources()
  }

  private def loadFromExternalConfig(): Map[Subsystem, List[KeywordConfig]] = {
    externalConfigPath
      .filter(filePath =>
        Subsystem.values.exists(subsystem => filePath.getFileName.toString.toLowerCase.contains(subsystem.name.toLowerCase))
      )
      .map { path =>
        val subsystem =
          Subsystem.values.find(subsystem => path.getFileName.toString.toLowerCase.contains(subsystem.name.toLowerCase)).get
        val fileConfig = ConfigFactory.parseFile(path.toFile)
        val config     = ConfigFactory.load(fileConfig).getConfig("dms.metadata.collection")
        val keywords   = config.root().keySet().asScala.toList
        val headerConfigList = keywords.map { keyword =>
          val keywordConfig = config.getConfig(keyword)
          if (isConstantConfig(keywordConfig)) readConstantConfig(keyword, keywordConfig)
          else readComplexConfig(keyword, keywordConfig)
        }
        subsystem -> headerConfigList
      }
      .toMap
  }

  private def loadFromResources(): Map[Subsystem, List[KeywordConfig]] = {
    subsystems.map { subsystem =>
      val instrumentConfig =
        ConfigFactory.parseResources(s"${subsystem.name}-keyword-mappings.conf").getConfig("dms.metadata.collection")
      val keywords = instrumentConfig.root().keySet().asScala.toList
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

package dms.metadata.collection.core

import csw.params.core.generics.Parameter
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import dms.metadata.collection.config.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}
import dms.metadata.collection.config.{FitsValue, KeywordConfig, ParamPath}

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable

class KeywordValueExtractor(headerConfigs: Map[Subsystem, List[KeywordConfig]]) {

  def extractKeywordValuesFor(
      configSubsystem: Subsystem,
      obsEventToProcess: Event,
      snapshot: ConcurrentHashMap[EventKey, Event]
  ): Map[String, String] =
    headerConfigs(configSubsystem)
      .filter(_.obsEventName == obsEventToProcess.eventName.name)
      .map {
        case config: ComplexKeywordConfig          => (config.keyword, extract(config, snapshot))
        case ConstantKeywordConfig(keyword, value) => (keyword, Some(value))
      }
      .collect { case (keyword, Some(value)) => keyword -> value }
      .toMap

  private def getParam(path: ParamPath, paramSet: Set[Parameter[_]]): Option[Parameter[_]] = {
    def traverseNonStruct(paramPath: ParamPath) =
      paramSet.find(_.keyName == paramPath.keyName).map { p =>
        val paramOfAny = p.asInstanceOf[Parameter[Any]]
        paramOfAny.copy(items = mutable.ArraySeq(paramOfAny.items(paramPath.index)))
      }

    if (path.keyName.isBlank) return None
    traverseNonStruct(path)
  }

  private def extract(
      config: ComplexKeywordConfig,
      snapshot: ConcurrentHashMap[EventKey, Event] // todo: should we change signature?
  ): Option[String] =
    Option(snapshot.get(EventKey(config.eventKey)))
      .flatMap(e => getParam(config.paramPath, e.paramSet))
      .map(p => extractValueFromParam(p.items.head, config))

  private def extractValueFromParam(value: Any, config: ComplexKeywordConfig): String = {
    val attribute = config.attribute.getOrElse(FitsValue.Default)
    FitsValue.attributeFormats(value)(attribute)
  }
}

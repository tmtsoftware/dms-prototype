package dms.metadata.collection.core

import java.util.concurrent.ConcurrentHashMap

import csw.params.core.generics.Parameter
import csw.params.core.models.Struct
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import dms.metadata.collection.config.KeywordConfig.{ComplexKeywordConfig, ConstantKeywordConfig}
import dms.metadata.collection.config.{FitsValue, KeywordConfig, ParamPath}

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

  private def getParam(path: List[ParamPath], paramSet: Set[Parameter[_]]): Option[Parameter[_]] = {
    def traverseStruct(paramPath: ParamPath) =
      paramSet.find(_.keyName == paramPath.keyName).flatMap { param =>
        param.get(paramPath.index).map(x => x.asInstanceOf[Struct].paramSet)
      }

    def traverseNonStruct(paramPath: ParamPath) =
      paramSet.find(_.keyName == paramPath.keyName).map { p =>
        val paramOfAny = p.asInstanceOf[Parameter[Any]]
        paramOfAny.copy(items = mutable.ArraySeq(paramOfAny.items(paramPath.index)))
      }

    path match {
      case Nil          => None
      case head :: Nil  => traverseNonStruct(head)
      case head :: next => traverseStruct(head).flatMap(getParam(next, _))
    }
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

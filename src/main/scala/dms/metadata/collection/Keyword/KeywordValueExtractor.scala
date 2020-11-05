package dms.metadata.collection.Keyword

import java.util.concurrent.ConcurrentHashMap

import csw.params.core.generics.Parameter
import csw.params.core.models.Struct
import csw.params.events.{Event, EventKey}
import dms.metadata.collection.Keyword.KeywordConfig.ComplexKeywordConfig

import scala.collection.mutable
case class FitsKeyword(keyword: String)

class KeywordValueExtractor {

  private def getParam(path: List[ParamPath], paramSet: Set[Parameter[_]]): Option[Parameter[_]] = {
    path match { //FIXME match may not be exhaustive. It would fail on the following input: Nil
      case head :: Nil =>
        paramSet.find(_.keyName == head.path).map { p =>
          val paramOfAny = p.asInstanceOf[Parameter[Any]]
          paramOfAny.copy(items = mutable.ArraySeq(paramOfAny.items(head.index)))
        }
      case head :: next =>
        paramSet.find(_.keyName == head.path).flatMap { param =>
          param.get(head.index).flatMap(x => getParam(next, x.asInstanceOf[Struct].paramSet))
        }
    }
  }

  val defaultPrimaryExtractors: PartialFunction[(Any, FitsKeyword), String] = {
    case x @ (Byte | Short | Long | Int | Float | Boolean | Char, _) => x._1.toString
  }

  val defaultExtractor: PartialFunction[(Any, FitsKeyword), String] = { case _ => "" /* fixme : add error*/ }

  def extractValueFromParam3(value: Any, config: ComplexKeywordConfig): String = {
    val handler = FitsValueExtractor.extract orElse defaultPrimaryExtractors orElse defaultExtractor
    handler.apply(value -> FitsKeyword(config.keyword))
  } // error

  def extract(
      config: ComplexKeywordConfig,
      snapshot: ConcurrentHashMap[EventKey, Event] // todo: should we change signature?
  ): Option[String] = {
    val paramValue = Option(snapshot.get(EventKey(config.eventKey)))
      .flatMap(e => getParam(config.paramPath, e.paramSet))
      .map { p => extractValueFromParam3(p.items.head, config) }

    paramValue
  }
}

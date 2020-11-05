package dms.metadata.collection.Keyword

import java.util.concurrent.ConcurrentHashMap

import com.jayway.jsonpath.JsonPath
import csw.params.core.generics.Parameter
import csw.params.core.models.Struct
import csw.params.events.{Event, EventKey}
import dms.metadata.collection.Keyword.KeywordConfig.ComplexKeywordConfig
import io.bullet.borer.{Encoder, Json}

import scala.collection.mutable

class KeywordValueExtractor {

  def extract(
      config: ComplexKeywordConfig,
      snapshot: ConcurrentHashMap[EventKey, Event] // todo: should we change signature?
  ): String = {
    val paramValue = Option(snapshot.get(EventKey(config.eventKey)))
      .flatMap(e => getParam(config.paramPath, e.paramSet))
      .map { p =>
        val encoder            = p.keyType._arraySeqCodec.encoder.asInstanceOf[Encoder[mutable.ArraySeq[Any]]]
        val encodedParamValues = Json.encode(p.items.asInstanceOf[mutable.ArraySeq[Any]])(encoder).toUtf8String
        extractValueFromParam(encodedParamValues, config)
      }
    require(paramValue.nonEmpty) // FIXME should we fail/use some default value
    paramValue.get
  }

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

  private def extractValueFromParam(encodedParam: String, config: ComplexKeywordConfig): String = { // fixme: make this Options
    val jsonPathWithIndex = "$[0]" + config.fieldPath.getOrElse("") // take 0th as list has only one item.
    JsonPath.read[Any](s"$encodedParam", jsonPathWithIndex).toString
  }
}

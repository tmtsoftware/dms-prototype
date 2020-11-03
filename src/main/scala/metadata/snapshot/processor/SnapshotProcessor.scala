package metadata.snapshot.processor

import java.util.concurrent.ConcurrentHashMap

import com.jayway.jsonpath.JsonPath
import csw.params.core.generics.Parameter
import csw.params.core.models.Struct
import csw.params.events.{Event, EventKey}
import io.bullet.borer.{Encoder, Json}
import metadata.snapshot.processor.HeaderConfig.ComplexConfig

import scala.collection.mutable

class SnapshotProcessor {
  def getParam(path: List[ParamPath], paramSet: Set[Parameter[_]]): Option[Parameter[_]] = {
    path match {
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

  def extractValueFromParam(encodedParam: String, config: ComplexConfig): String = { // fixme: make this Options
    val jsonPathWithIndex = "$[0]" + config.fieldPath.getOrElse("") // take 0th as list has only one item.
    JsonPath.read[Any](s"$encodedParam", jsonPathWithIndex).toString
  }

  def getHeader(
      config: ComplexConfig,
      snapshot: ConcurrentHashMap[EventKey, Event] // todo: should we change signature?
  ): Option[String] = {
    val paramValue = Option(snapshot.get(EventKey(config.eventKey)))
      .flatMap(e => getParam(config.paramPath, e.paramSet))
      .map { p =>
        val encoder            = p.keyType._arraySeqCodec.encoder.asInstanceOf[Encoder[mutable.ArraySeq[Any]]]
        val encodedParamValues = Json.encode(p.items.asInstanceOf[mutable.ArraySeq[Any]])(encoder).toUtf8String
        extractValueFromParam(encodedParamValues, config)
      }

    paramValue
  }
}

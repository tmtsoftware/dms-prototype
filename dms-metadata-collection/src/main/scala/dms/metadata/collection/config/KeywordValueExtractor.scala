package dms.metadata.collection.config

import java.util.concurrent.ConcurrentHashMap

import csw.params.core.generics.Parameter
import csw.params.core.models.Coords._
import csw.params.core.models.{RaDec, Struct}
import csw.params.events.{Event, EventKey}
import csw.time.core.models.{TAITime, UTCTime}
import dms.metadata.collection.config.KeywordConfig.ComplexKeywordConfig

import scala.collection.mutable
import scala.util.chaining.scalaUtilChainingOps
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

  def extract(
      config: ComplexKeywordConfig,
      snapshot: ConcurrentHashMap[EventKey, Event] // todo: should we change signature?
  ): Option[String] = {
    val paramValue = Option(snapshot.get(EventKey(config.eventKey)))
      .flatMap(e => getParam(config.paramPath, e.paramSet).tap(println))
      .map { p => extractValueFromParam(p.items.head, config) }

    paramValue
  }

  def extract(value: Any): Map[String, () => String] = {
    import FitsValueFormats._
    value match {
      case x: EqCoord    => eqCoordFormats.format(x)
      case x: AltAzCoord => altAzCoordFormats.format(x)
      case x: Double     => doubleFormats.format(x)
      case x: UTCTime    => utcTimeFormats.format(x)

      // ---------------------------------

      case x: RaDec            => raDecFormats.format(x)
      case x: SolarSystemCoord => solarSystemCoordFormats.format(x)
      case x: MinorPlanetCoord => minorPlanetCoordFormats.format(x)
      case x: CometCoord       => cometCoordFormats.format(x)
//      case x: Coord            => "" // fixme: coord is a sealed trait. why do we have a key for that.

      case x: String  => stringFormats.format(x)
      case x: Struct  => structFormats.format(x)
      case x: TAITime => taiTimeFormats.format(x)

      case x: Boolean => booleanFormats.format(x)
      case x: Char    => charFormats.format(x)
      case x: Byte    => byteFormats.format(x)
      case x: Short   => shortFormats.format(x)
      case x: Long    => longFormats.format(x)
      case x: Int     => intFormats.format(x)
      case x: Float   => floatFormats.format(x)

//      case x: ArrayData[Byte]   => ""
      //      case x: ArrayData[Short]  => ""
      //      case x: ArrayData[Long]   => ""
      //      case x: ArrayData[Int]    => ""
      //      case x: ArrayData[Float]  => ""
      //      case x: ArrayData[Double] => ""
      //
      //      case x: MatrixData[Byte]   => ""
      //      case x: MatrixData[Short]  => ""
      //      case x: MatrixData[Long]   => ""
      //      case x: MatrixData[Int]    => ""w
      //      case x: MatrixData[Float]  => ""
      //      case x: MatrixData[Double] => ""
    }
  }

  def extractValueFromParam(value: Any, config: ComplexKeywordConfig): String = {
    val str = config.attribute.getOrElse(KeywordValueExtractor.DEFAULT)
    extract(value)(str)()
  }
}

object KeywordValueExtractor {
  val DEFAULT = "default"
}

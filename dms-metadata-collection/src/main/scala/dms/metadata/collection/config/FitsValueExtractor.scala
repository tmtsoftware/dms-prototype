package dms.metadata.collection.config

import csw.params.core.models.Angle
import csw.params.core.models.Coords._
import csw.time.core.models.{TAITime, UTCTime}

object FitsValueExtractor {
  def extract: PartialFunction[Any, String] = {
    case (x: AltAzCoord, FitsKeyword("ALT")) => x.alt.toDegree.toString
    case (x: AltAzCoord, FitsKeyword("AZ"))  => x.az.toDegree.toString
    case (x: EqCoord, FitsKeyword("RA"))     => Angle.raToString(x.ra.toRadian)
    case (x: EqCoord, FitsKeyword("DEC"))    => Angle.deToString(x.dec.toRadian)
    //-------------

    case (x: SolarSystemCoord, FitsKeyword("")) => x.toString
    case (x: MinorPlanetCoord, FitsKeyword("")) => x.toString
    case (x: CometCoord, FitsKeyword(""))       => x.toString
    case (x: UTCTime, FitsKeyword(""))          => x.toString
    case (x: TAITime, FitsKeyword(""))          => x.toString
    case (x: Double, FitsKeyword(""))           => x.toString
//    case (x: ArrayData[Int], "")                => x.data
  }
}

object A {

  def attributeMapForAny(x: Any) = {
    x match {
      case a: AltAzCoord => attributeMap(a)
      case a: EqCoord    => attributeMap(a)
      case a: Double     => a.toString
    }
  }

//  _-------------------------------------------

  def attributeMap(x: AltAzCoord): Map[String, String] = {
    Map(
      "altitude"  -> x.alt.toRadian.toString,
      "altitudeD" -> x.alt.toDegree.toString,
      "azimuth"   -> x.az.toRadian.toString,
      "azimuthD"  -> x.az.toDegree.toString
    )
  }

  def attributeMap(x: EqCoord): Map[String, String] = {
    Map(
      "ra"  -> x.ra.toRadian.toString,
      "dec" -> x.dec.toRadian.toString
    )
  }
//  same formatting for all the primitives
  def attributeMap(x: Double): Map[String, String] = {
    Map(
      "default" -> x.toString,
      "custom1" -> x.toString
    )
  }
//  _________________________________________-

  trait FitsFormat[T] {
    def format(value: T): Map[String, String]
  }

  val altAzCoordFormat: FitsFormat[AltAzCoord] = (x: AltAzCoord) =>
    Map(
      "azimuth"  -> x.az.toRadian.toString,
      "altitude" -> x.alt.toRadian.toString,
      "azimuthD" -> x.az.toDegree.toString
    )

}

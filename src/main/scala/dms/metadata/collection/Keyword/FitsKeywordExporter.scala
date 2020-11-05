package dms.metadata.collection.Keyword

import csw.params.core.models.Angle
import csw.params.core.models.Coords._
import csw.time.core.models.{TAITime, UTCTime}

object FitsKeywordExporter {
  def export: PartialFunction[Any, String] = {
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

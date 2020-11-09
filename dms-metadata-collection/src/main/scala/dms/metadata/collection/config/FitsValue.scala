package dms.metadata.collection.config

import java.time.format.DateTimeFormatter

import csw.params.core.models.Coords._
import csw.time.core.models.UTCTime

object FitsValue {
  def attributeFormats(value: Any): Map[String, String] = {
    value match {
      case x: EqCoord =>
        Map(
          "ra"  -> x.ra.toRadian.toString,
          "dec" -> x.dec.toRadian.toString
        )
      case x: AltAzCoord =>
        Map(
          "az"   -> x.az.toRadian.toString,
          "alt"  -> x.alt.toRadian.toString,
          "azD"  -> f"${x.alt.toDegree}%.3f",
          "altD" -> f"${x.alt.toDegree}%.3f"
        )
      case x: Double =>
        Map(
          Default  -> x.toString, // Fixme: required? default case is handled in catch all case
          "custom" -> f"$x%.3f"
        )
      case x: UTCTime =>
        Map(
          Default -> x.toString, // Fixme: required? default case is handled in catch all case
          "iso"   -> DateTimeFormatter.ISO_INSTANT.format(x.value)
        )
      case x =>
        Map(
          Default -> x.toString
        )
    }
  }

  lazy val Default = "default"
}

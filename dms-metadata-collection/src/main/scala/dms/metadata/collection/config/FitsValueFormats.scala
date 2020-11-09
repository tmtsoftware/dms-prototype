package dms.metadata.collection.config

import java.time.format.DateTimeFormatter

import csw.params.core.models.Coords._
import csw.params.core.models.{RaDec, Struct}
import csw.time.core.models.{TAITime, UTCTime}

trait FitsFormat[T] {
  def format(value: T): Map[String, () => String]
}

object FitsValueFormats {
  import KeywordValueExtractor.DEFAULT

  val eqCoordFormats: FitsFormat[EqCoord] = (x: EqCoord) =>
    Map(
      "ra"  -> x.ra.toRadian.toString,
      "dec" -> x.dec.toRadian.toString
    )

  val altAzCoordFormats: FitsFormat[AltAzCoord] = (x: AltAzCoord) =>
    Map(
      "azimuth"   -> x.az.toRadian.toString,
      "altitude"  -> x.alt.toRadian.toString,
      "azimuthD"  -> threeDigitPrecise(x.az.toDegree).toString,
      "altitudeD" -> threeDigitPrecise(x.alt.toDegree).toString
    )

  val doubleFormats: FitsFormat[Double] = (x: Double) => {
    Map(
      DEFAULT  -> x.toString,
      "custom" -> threeDigitPrecise(x).toString
    )
  }

  val utcTimeFormats: FitsFormat[UTCTime] = (x: UTCTime) => {
    Map(
      DEFAULT -> x.toString,
      "iso"   -> DateTimeFormatter.ISO_INSTANT.format(x.value).toString
    )
  }

  val raDecFormats: FitsFormat[RaDec] = (x: RaDec) => {
    Map(DEFAULT -> x.toString)
  }
  val solarSystemCoordFormats: FitsFormat[SolarSystemCoord] = (x: SolarSystemCoord) => {
    Map(DEFAULT -> x.toString)
  }
  val minorPlanetCoordFormats: FitsFormat[MinorPlanetCoord] = (x: MinorPlanetCoord) => {
    Map(DEFAULT -> x.toString)
  }
  val cometCoordFormats: FitsFormat[CometCoord] = (x: CometCoord) => {
    Map(DEFAULT -> x.toString)
  }

  val stringFormats: FitsFormat[String] = (x: String) => {
    Map(DEFAULT -> (() => x))
  }
  val structFormats: FitsFormat[Struct] = (x: Struct) => {
    Map(DEFAULT -> x.toString)
  }

  val taiTimeFormats: FitsFormat[TAITime] = (x: TAITime) => {
    Map(DEFAULT -> x.toString)
  }
  val booleanFormats: FitsFormat[Boolean] = (x: Boolean) => {
    Map(DEFAULT -> x.toString)
  }
  val charFormats: FitsFormat[Char] = (x: Char) => {
    Map(DEFAULT -> x.toString)
  }
  val byteFormats: FitsFormat[Byte] = (x: Byte) => {
    Map(DEFAULT -> x.toString)
  }
  val shortFormats: FitsFormat[Short] = (x: Short) => {
    Map(DEFAULT -> x.toString)
  }
  val longFormats: FitsFormat[Long] = (x: Long) => {
    Map(DEFAULT -> x.toString)
  }
  val intFormats: FitsFormat[Int] = (x: Int) => {
    Map(DEFAULT -> x.toString)
  }
  val floatFormats: FitsFormat[Float] = (x: Float) => {
    Map(DEFAULT -> x.toString)
  }

  // todo: add entries for array keys
  // todo: add entries for matrix keys

  private def makePrecise(double: Double, precision: Int): Double = {
    val d = Math.pow(10, precision)
    (double * d).round / d
  }

  private def threeDigitPrecise(double: Double) = makePrecise(double, 3)
}

package metadata.snapshot.processor

import com.jayway.jsonpath.JsonPath
import csw.params.core.generics.KeyType._
import csw.params.core.generics.Parameter
import csw.params.core.models.{ArrayData, RaDec, Struct}
import io.bullet.borer.{Encoder, Json}

import scala.collection.mutable

object SnapshotProcessor2 extends App {
  private val paramSet: Set[Parameter[_]] = Set(
    StructKey
      .make("struct1")
      .set(
        Struct().madd(
          StructKey
            .make("struct2")
            .setAll(
              Array(
                Struct().add(RaDecKey.make("radec3").set(RaDec(1, 243))),
                Struct().madd(RaDecKey.make("radec4").set(RaDec(1, 333)), IntKey.make("int").setAll(Array(12, 3, 4)))
              )
            ),
          StructKey
            .make("struct3")
            .setAll(
              Array(
                Struct().add(RaDecKey.make("radec5").set(RaDec(1, 234567), RaDec(ra = 1, dec = 999))),
                Struct().madd(RaDecKey.make("radec6").set(RaDec(111, 222)), IntKey.make("int").setAll(Array(12, 3, 4)))
              )
            )
        )
      ),
    RaDecKey.make("radec6").set(RaDec(333, 444)),
    IntKey.make("int").set(555, 666),
    StringKey.make("string").set("abc", "def"),
    IntArrayKey.make("intArray").set(ArrayData(mutable.ArraySeq(1, 2, 333)))
  )

  val keywordConfig = KeywordConfig(
    "AZIMUTH",
    "endExposure",
    "esw.filter_wheel.set_temp",
    "struct1/struct3[1]/radec6[0]",
    "ra"
  )

  val keywordConfig2 = KeywordConfig(
    "AZIMUTH",
    "endExposure",
    "esw.filter_wheel.set_temp",
    "radec6[0]",
    "dec"
  )

  val keywordConfig3 = KeywordConfig(
    "AZIMUTH",
    "endExposure",
    "esw.filter_wheel.set_temp",
    "int[1]"
  )
  val keywordConfig4 = KeywordConfig(
    "AZIMUTH",
    "endExposure",
    "esw.filter_wheel.set_temp",
    "string"
  )

  val keywordConfig5 = KeywordConfig(
    "AZIMUTH",
    "endExposure",
    "esw.filter_wheel.set_temp",
    "intArray",
    "[2]"
  )

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

  def extractValueFromParam(encodedParam: String, config: KeywordConfig) = {
    val jsonPathWithIndex = "$[0]" + config.jsonPath.getOrElse("") // take 0th as list has only one item.
    JsonPath.read[Any](s"$encodedParam", jsonPathWithIndex)
  }

  def getHeader(keywordConfig: KeywordConfig) =
    getParam(keywordConfig.paramPath, paramSet).map { p =>
      val en: Encoder[mutable.ArraySeq[Any]] = p.keyType._arraySeqCodec.encoder.asInstanceOf[Encoder[mutable.ArraySeq[Any]]]
      val encodedParamValues                 = Json.encode(p.items.asInstanceOf[mutable.ArraySeq[Any]])(en).toUtf8String

      extractValueFromParam(encodedParamValues, keywordConfig)
    }

  println("expected 111 = actual " + getHeader(keywordConfig))
  println("expected 444 = actual " + getHeader(keywordConfig2))
  println("expected 666 = actual " + getHeader(keywordConfig3))
  println("expected abc = actual " + getHeader(keywordConfig4))
  println("expected 333 = actual " + getHeader(keywordConfig5))
}

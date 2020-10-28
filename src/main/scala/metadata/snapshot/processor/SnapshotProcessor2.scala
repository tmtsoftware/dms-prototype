package metadata.snapshot.processor

import com.jayway.jsonpath.JsonPath
import csw.params.core.generics.KeyType.{IntKey, RaDecKey, StructKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.{RaDec, Struct}
import io.bullet.borer.Json

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
    IntKey.make("int").set(555, 666)
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
    "int[1]",
    ""
  )

  def getParam(path: List[ParamPath], paramSet: Set[Parameter[_]]): Option[Parameter[_]] = {
    path match {
      case head :: Nil => paramSet.find(_.keyName == head.path)
      case head :: next =>
        paramSet.find(_.keyName == head.path).flatMap { param =>
          param.get(head.index).flatMap(x => getParam(next, x.asInstanceOf[Struct].paramSet))
        }
    }
  }

  //  def extractValueFromParam(encodedParam: String, jsonPath: String): java.util.List[String] =
  //    JsonPath.read(s"$encodedParam", "$[0].*.values[1]" + jsonPath) // add index too

  def extractValueFromParam1(encodedParam: String, config: KeywordConfig): java.util.List[String] = {
    val jsonPath = "$[0].*.values[" + config.paramPath.last.index + "]" + config.jsonPath
//    println(jsonPath)
    JsonPath.read(s"$encodedParam", jsonPath) // add index too
  }

  def getHeader(keywordConfig: KeywordConfig) = {
    import csw.params.core.formats.ParamCodecs._

    getParam(keywordConfig.paramPath, paramSet).map { p =>
      val value: Set[Parameter[_]] = Set(p)
      val encodedParam             = Json.encode(value).toUtf8String
      extractValueFromParam1(encodedParam, keywordConfig)
    }
  }

  println("expected 111 = actual ", getHeader(keywordConfig))
  println("expected 444 = actual ", getHeader(keywordConfig2))
  println("expected 666 = actual ", getHeader(keywordConfig3))
}

package metadata.jsonpath

import com.jayway.jsonpath.JsonPath
import csw.params.core.formats.FlatParamCodecs._
import csw.params.core.models.Struct
import io.bullet.borer.Json
import net.minidev.json.JSONArray

object Main extends App {
  private val struct = Struct(SampleData.paramSet)
  private val json   = Json.encode(struct).toUtf8String

  private val azimuth            = JsonPathBuilder.build("Struct1/Struct11/Struct111/Azimuth", Some("az"))
  private val radec              = JsonPathBuilder.build("Struct1/Struct11/Struct111/RaDec")
  private val radec1             = JsonPathBuilder.build("Struct1/Struct11/Struct111/RaDec[1]")
  private val radec2WithIndex    = JsonPathBuilder.build("Struct1[1]/Struct12/RaDec2")
  private val radec2WithoutIndex = JsonPathBuilder.build("Struct1/Struct12/RaDec2")
  private val radec3TopLevel     = JsonPathBuilder.build("RaDec3")

  def read(jsonPath: String) = JsonPath.read[JSONArray](json, jsonPath).get(0)

  println("AZIMUTH json path ===>")
  println(azimuth)
  println("=" * 80)

  println(read(azimuth))
  println(read(radec))
  println(read(radec1))
  println(read(radec2WithIndex))
  println(read(radec2WithoutIndex))
  println(read(radec3TopLevel))

}

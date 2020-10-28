package metadata.jsonpath

import java.io.File

import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray

object Main extends App {
  private val jsonFile = new File("src/main/scala/metadata/jsonpath/sample.json")

  private val azimuth            = JsonPathBuilder.build("S1/S11/S111/Azimuth", Some("az"))
  private val radec2WithIndex    = JsonPathBuilder.build("S1[1]/S12/RaDec2")
  private val radec2WithoutIndex = JsonPathBuilder.build("S1/S12/RaDec2")
  private val radec3TopLevel     = JsonPathBuilder.build("RaDec3")

  def read(jsonPath: String) = JsonPath.read[JSONArray](jsonFile, jsonPath).get(0)

  println(read(azimuth))
  println(read(radec2WithIndex))
  println(read(radec2WithoutIndex))
  println(read(radec3TopLevel))

}

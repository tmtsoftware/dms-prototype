package exp.data

import java.nio.file.{Files, Path}

import io.bullet.borer.Dom.Element
import io.bullet.borer.Json

object ParamSetJson {
  // doing a pass through borer to get single line representation, better alternatives are possible
  val jsonString: String = {
    val str   = Files.readString(Path.of("paramSet.json"))
    val value = Json.decode(str.getBytes()).to[Element].value
    Json.encode(value).toUtf8String
  }
}

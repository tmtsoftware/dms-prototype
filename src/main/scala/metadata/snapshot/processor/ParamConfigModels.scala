package metadata.snapshot.processor

case class KeywordConfig(
    keyword: String,
    obsEventName: String,
    eventKey: String,
    paramPath: List[ParamPath],
    jsonPath: Option[String]
)

object KeywordConfig {
  def apply(
      keyword: String,
      obsEventName: String,
      eventKey: String,
      paramPath: String,
      jsonPath: String
  ): KeywordConfig = new KeywordConfig(keyword, obsEventName, eventKey, ParamConfigParser.from(paramPath), Some(jsonPath))

  def apply(
      keyword: String,
      obsEventName: String,
      eventKey: String,
      paramPath: String
  ): KeywordConfig = new KeywordConfig(keyword, obsEventName, eventKey, ParamConfigParser.from(paramPath), None)
}

case class ParamPath(path: String, index: Int = 0)

object ParamConfigParser {
  private val pattern = """(.*)\[(\d+)\]""".r

  def from(path: String): List[ParamPath] =
    if (path.isBlank) List.empty else path.split("/").toList.map(toParamPath)

  private def toParamPath(name: String): ParamPath =
    name match {
      case pattern(name, index) => ParamPath(name, index.toInt)
      case _                    => ParamPath(name)
    }
}

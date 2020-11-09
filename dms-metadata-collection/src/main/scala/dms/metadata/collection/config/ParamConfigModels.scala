package dms.metadata.collection.config

sealed trait KeywordConfig extends Product {
  def keyword: String;
  def obsEventName: String;
}

object KeywordConfig {
  case class ComplexKeywordConfig(
      keyword: String,
      obsEventName: String,
      eventKey: String,
      private val paramKey: String,
      attribute: Option[String]
  ) extends KeywordConfig {
    val paramPath: List[ParamPath] = ParamConfigParser.from(paramKey)
  }

  final case class ConstantKeywordConfig(keyword: String, value: String) extends KeywordConfig {
    val obsEventName = "exposureStart" //FIXME read value from config or find better approach
  }
}

case class ParamPath(keyName: String, index: Int = 0)

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

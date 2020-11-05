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
      paramPath: List[ParamPath],
      fieldPath: Option[String]
  ) extends KeywordConfig

  final case class ConstantKeywordConfig(keyword: String, value: String) extends KeywordConfig {
    val obsEventName = "exposureStart" //FIXME read value from config or find better approach
  }

  object ComplexKeywordConfig {
    def apply(
        keyword: String,
        obsEventName: String,
        eventKey: String,
        paramPath: String,
        fieldPath: String
    ): ComplexKeywordConfig =
      new ComplexKeywordConfig(keyword, obsEventName, eventKey, ParamConfigParser.from(paramPath), Some(fieldPath))

    def apply(
        keyword: String,
        obsEventName: String,
        eventKey: String,
        paramPath: String
    ): ComplexKeywordConfig = new ComplexKeywordConfig(keyword, obsEventName, eventKey, ParamConfigParser.from(paramPath), None)
  }
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

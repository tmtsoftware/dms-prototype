package metadata.snapshot.processor

sealed trait HeaderConfig extends Product {
  def keyword: String;
  def obsEventName: String;
}

object HeaderConfig {
  case class ComplexConfig(
      keyword: String,
      obsEventName: String,
      eventKey: String,
      paramPath: List[ParamPath],
      fieldPath: Option[String]
  ) extends HeaderConfig

  final case class SimpleConfig(keyword: String, value: String) extends HeaderConfig {
    val obsEventName = "exposureStart" //FIXME read value from config or find better approach
  }

  object ComplexConfig {
    def apply(
        keyword: String,
        obsEventName: String,
        eventKey: String,
        paramPath: String,
        fieldPath: String
    ): ComplexConfig = new ComplexConfig(keyword, obsEventName, eventKey, ParamConfigParser.from(paramPath), Some(fieldPath))

    def apply(
        keyword: String,
        obsEventName: String,
        eventKey: String,
        paramPath: String
    ): ComplexConfig = new ComplexConfig(keyword, obsEventName, eventKey, ParamConfigParser.from(paramPath), None)
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

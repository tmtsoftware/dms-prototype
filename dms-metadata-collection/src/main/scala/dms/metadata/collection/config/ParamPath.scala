package dms.metadata.collection.config

case class ParamPath(path: String, index: Int = 0)

object ParamPath {
  private val pattern = """(.*)\[(\d+)\]""".r

  def from(path: String): List[ParamPath] =
    if (path.isEmpty) List.empty else path.split("/").toList.map(toParamPath)

  private def toParamPath(name: String): ParamPath =
    name match {
      case pattern(name, index) => ParamPath(name, index.toInt)
      case _                    => ParamPath(name)
    }
}

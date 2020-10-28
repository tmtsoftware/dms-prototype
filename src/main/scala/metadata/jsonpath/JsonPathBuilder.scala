package metadata.jsonpath

object JsonPathBuilder {

  def build(keyPath: String, valuePath: Option[String] = None): String = {
    val keyNames    = keyPath.split("/").toList
    val fullKeyPath = jsonPath(keyNames).mkString(".")
    valuePath.map(fullKeyPath + "." + _).getOrElse(fullKeyPath)
  }

  private def jsonPath(keys: List[String]): List[String] =
    keys.zipWithIndex.map { case (k, i) => jsonPath(k, keys.length - 1 == i) }

  private def jsonPath(keyName: String, last: Boolean): String = {
    val (name, index) = extractNameAndIndex(keyName)

    val valuesPredicate = index.getOrElse(if (last) "0" else "?(@.paramSet)")
    s"""paramSet[?(@.keyName == "$name")].values[$valuesPredicate]"""
  }

  private val NameAndIndex = """(.*)\[(\d+)\]""".r
  private def extractNameAndIndex(key: String) =
    key match {
      case NameAndIndex(n, i) => (n, Some(i))
      case _                  => (key, None)
    }

}

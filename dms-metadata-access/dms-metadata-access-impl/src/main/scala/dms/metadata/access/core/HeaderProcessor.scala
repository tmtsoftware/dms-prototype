package dms.metadata.access.core

import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Subsystem
import nom.tam.fits.Header

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.file.Path
import scala.jdk.CollectionConverters.CollectionHasAsScala

class HeaderProcessor(externalConfigPath: Option[Path]) {

  def loadHeaderList(): Map[Subsystem, List[String]] = {
    if (externalConfigPath.isDefined) loadHeaderListFromResourceConf() ++ loadHeaderListFromExternalConf()
    else loadHeaderListFromResourceConf()
  }

  private def loadHeaderListFromExternalConf(): Map[Subsystem, List[String]] = {
    val fileConfig = ConfigFactory.parseFile(externalConfigPath.get.toFile)
    val config     = ConfigFactory.load(fileConfig)
    config
      .getConfig("dms.metadata.access")
      .entrySet()
      .asScala
      .filter(e => Subsystem.values.exists(s => s.name.equalsIgnoreCase(e.getKey)))
      .map(e =>
        Subsystem.upperCaseNameValuesToMap(e.getKey.toUpperCase) ->
          e.getValue.atKey(e.getKey).getStringList(e.getKey).asScala.toList
      )
      .toMap
  }

  private def loadHeaderListFromResourceConf(): Map[Subsystem, List[String]] = {
    val config: Config = ConfigFactory.parseResources("header-keywords.conf")
    config
      .resolve()
      .getConfig("dms.metadata.access")
      .entrySet()
      .asScala
      .filter(e => Subsystem.values.exists(s => s.name.equalsIgnoreCase(e.getKey)))
      .map(e =>
        Subsystem.upperCaseNameValuesToMap(e.getKey.toUpperCase) ->
          e.getValue.atKey(e.getKey).getStringList(e.getKey).asScala.toList
      )
      .toMap
  }

  def generateFormattedHeader(keywords: Seq[String], keywordData: Map[String, String]): String = {
    val fitsHeader = new Header()
    keywords.foreach { keyword =>
      keywordData.get(keyword).map(fitsHeader.addValue(keyword, _, ""))
    //FIXME what to do if keyword not found in keywordData received from db
    }

    val os = new ByteArrayOutputStream();
    val ps = new PrintStream(os);

    fitsHeader.dumpHeader(ps)
    os.toString("UTF8")
  }

}

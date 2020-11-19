package dms.metadata.access.core

import java.io.{ByteArrayOutputStream, PrintStream}

import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.{IRIS, WFOS}
import nom.tam.fits.Header

import scala.jdk.CollectionConverters.CollectionHasAsScala

class HeaderProcessor {

  def loadHeaderList(): Map[Subsystem, List[String]] = {
    val config: Config              = ConfigFactory.parseResources("header-keywords.conf")
    val subsystems: List[Subsystem] = List(IRIS, WFOS) // FIXME Should this be passed as config/ or consider all subsystem
    subsystems.map { subsystem =>
      subsystem -> config.resolve().getStringList(subsystem.name).asScala.toList
    }.toMap
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

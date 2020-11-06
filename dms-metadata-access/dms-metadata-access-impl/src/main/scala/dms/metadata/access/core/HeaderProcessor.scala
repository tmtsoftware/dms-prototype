package dms.metadata.access.core

import java.io.{ByteArrayOutputStream, File, PrintStream}

import com.typesafe.config.{Config, ConfigFactory}
import csw.prefix.models.Subsystem
import nom.tam.fits.Header

import scala.jdk.CollectionConverters.CollectionHasAsScala

class HeaderProcessor {

  def loadHeaderList(): Map[Subsystem, List[String]] = {
    val path           = getClass.getResource("/header-keywords.conf").getPath
    val config: Config = ConfigFactory.parseFile(new File(path))
    val subsystemNames = config.root().keySet().asScala.toList
    subsystemNames.map { subsystemName =>
      val maybeSubsystem: Option[Subsystem] = Subsystem.values.toList.find(_.name == subsystemName)
      maybeSubsystem match {
        case Some(value) => value -> config.getStringList(value.name).asScala.toList
        case None        => throw new RuntimeException(s"Invalid Subsystem Name : $subsystemName received from header-keywords.conf")
      }
    }.toMap
  }

  def generateFormattedHeader(keywords: Seq[String], keywordData: Map[String, String]): String = {
    val fitsHeader = new Header()
    keywords.foreach { keyword =>
      keywordData.get(keyword).map { fitsHeader.addValue(keyword, _, "") }
    //FIXME what to do if keyword not found in keywordData received from db
    }

    val os = new ByteArrayOutputStream();
    val ps = new PrintStream(os);

    fitsHeader.dumpHeader(ps)

    val output = os.toString("UTF8")
    output
  }

}

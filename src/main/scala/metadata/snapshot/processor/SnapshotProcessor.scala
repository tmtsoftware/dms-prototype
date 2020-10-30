package metadata.snapshot.processor

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import io.bullet.borer.Json
import metadata.db.{EventRecord, ParamSetData}
import metadata.snapshot.processor.SnapshotProcessorUtil.mockHeaderList
import nom.tam.fits.Header

import scala.collection.MapView
import scala.jdk.CollectionConverters.CollectionHasAsScala

case class HeaderReadConfig(eventKey: EventKey, paramKey: String, obsEventName: String)
case class HeaderConfig(keyword: String, obsEventName: String, eventKey: String, paramKey: String, jsonPath: String)

object SnapshotProcessor extends App {
  val source: Prefix = Prefix(ESW, "filter")

  // insert 3 snapshots
  val snapshotsOfExposure =
    new ConcurrentHashMap[String, ConcurrentHashMap[EventKey, Event]]() // this can be String -> EventKey ->  paramSet

  List("startExposure", "midExposure", "endExposure").foreach(obsName =>
    snapshotsOfExposure.put(obsName, SnapshotProcessorUtil.createSnapshot(source))
  )

//  ----------------------------- process snapshot----------------------------------

  (1 to 50).foreach { _ =>
    val start: Long = System.currentTimeMillis()

    val keywords: List[HeaderConfig] = mockHeaderList()
    val headerData                   = SnapshotProcessorUtil.getHeaderData(snapshotsOfExposure)

    val formattedHeaders = SnapshotProcessorUtil.generateFormattedHeader(keywords, headerData)
    println("time : " + (System.currentTimeMillis() - start))
    println(headerData.size)
  }
//  --------------------------------------------------------------------------------

}

object SnapshotProcessorUtil {

  def generateFormattedHeader(headers: List[HeaderConfig], headerData: Map[String, Option[String]]) = {
    val fitsHeader = new Header()
    headers.foreach { header =>
      val maybeMaybeString = headerData.get(header.keyword).flatten
      fitsHeader.addValue(header.keyword, maybeMaybeString.get, "")
    }
//    headerData.foreach { case (keyword, Some(data)) => fitsHeader.addValue(keyword, data, "") }

    val os = new ByteArrayOutputStream();
    val ps = new PrintStream(os);

    fitsHeader.dumpHeader(ps)

    val output = os.toString("UTF8")
    output
  }

  def createSnapshot(source: Prefix): ConcurrentHashMap[EventKey, Event] = {
    val snapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap[EventKey, Event]()

    (1 to 2300).foreach { i =>
      val event = SystemEvent(source, EventName(s"event_key_$i"))
        .madd(ParamSetData.paramSet)
        .madd(StringKey.make(s"param_key_$i").set(s"param-value-$i"))
      snapshot.put(event.eventKey, event)
    }

    snapshot
  }

  def getHeaderData(
      snapshotsOfExposure: ConcurrentHashMap[String, ConcurrentHashMap[EventKey, Event]]
  ): Map[String, Option[String]] = {

    val headerConfig: MapView[String, HeaderReadConfig] = readHeaderConfig()

    val headerData: Map[String, Option[String]] = headerConfig.map {

      case (keyword, config) =>
        val maybeString = Option(snapshotsOfExposure.get(config.obsEventName))
          .flatMap(snapshot => Option(snapshot.get(config.eventKey)))
          .flatMap {
            case s: SystemEvent  => s.get(StringKey.make(config.paramKey)).map(_.head) // how would we know Ket type
            case o: ObserveEvent => o.get(StringKey.make(config.paramKey)).map(_.head) // how would we know Ket type
          }
        (keyword, maybeString)
    }.toMap
    headerData
  }

  def getHeaderData1(
      snapshot: Map[EventKey, EventRecord]
  ): List[(String, String)] = {
    val headerConfigs: List[HeaderConfig] = readHeaderConfigFlat()
    val headerData: List[(String, String)] = headerConfigs.map { headerConfig: HeaderConfig =>
      val value = snapshot
        .get(EventKey(headerConfig.eventKey))
        .map(e => e.paramSet)
        .get
        .find(p => p.keyName.equals(headerConfig.paramKey))
        .map(_.head match { case s: String => s })
        .get
      (headerConfig.keyword, value)
    }
    headerData
  }

  private def readHeaderConfig(): MapView[String, HeaderReadConfig] = {
    val path                      = getClass.getResource("/fitsHeaders.conf").getPath
    val configFileContent: Config = ConfigFactory.parseFile(new File(path))

    val configStr = configFileContent.getConfig("fits-keyword-config").root().render(ConfigRenderOptions.concise())

    val fitsHeaderConfig = Json.decode(configStr.getBytes).to[Map[String, String]].value

    fitsHeaderConfig.view.mapValues { s =>
      val (eveParam, obsEventName) = s.splitAt(s.indexOf("@"))
      val (eventKey, paramKey)     = eveParam.splitAt(eveParam.indexOf(":"))
      HeaderReadConfig(EventKey(eventKey), paramKey.drop(1), obsEventName.drop(1))
    }
  }

  def mockHeaderList(): List[HeaderConfig] = {
    List(
      "GRPLCn",
      "DATE-END",
      "ORIGIN",
      "HISTORY",
      "EXTVER",
      "EXTLEVEL",
      "DATE_OBS",
      "FRATIO",
      "PROGRAM",
      "GCOUNT",
      "OBS_ID",
      "DATAMIN",
      "RA",
      "FILTERn",
      "GRPIDn",
      "TDIMn",
      "DETNAM",
      "EPOCH",
      "PSCALn",
      "LATITUDE",
      "RATEL",
      "ELAPTIME",
      "GRPNAME",
      "ONTIME",
      "EXPTIME",
      "DATE-OBS",
      "CDELTn",
      "CROTAn",
      "LST",
      "DEC_NOM",
      "TUNITn",
      "NAXIS",
      "RA_PNT",
      "TFORMn",
      "TFIELDS",
      "SATURATE",
      "RA_SCZ",
      "MOONANGL",
      "ELTEL",
      "EXTNAME",
      "GRATINGn",
      "GRATING",
      "RA_OBJ",
      "DECTEL",
      "COMMENT",
      "CRPIXn",
      "TZEROn",
      "PA_PNT",
      "RA_NOM",
      "DEC_SCX",
      "EQUINOX",
      "INSTRUME",
      "TTYPEn",
      "EXTEND",
      "BLANK",
      "TNULLn",
      "TIME-END",
      "REFERENC",
      "XTENSION",
      "CONFIGUR",
      "RA_SCY",
      "BSCALE",
      "DEC_SXY",
      "TELESCOP",
      "DATE",
      "DEC_OBJ",
      "CTYPEn",
      "TBCOLn",
      "SIMPLE",
      "CREATOR",
      "CRVALn",
      "DATAMAX",
      "APERTURE",
      "PZEROn",
      "FILTER",
      "THEAP",
      "NAXISn",
      "RA_SCX",
      "EXPOSURE",
      "LIVETIME",
      "END",
      "DEC",
      "DEC_SCZ",
      "OBSERVER",
      "OBJNAME",
      "AUTHOR",
      "TSCALn",
      "BZERO",
      "PTYPEn",
      "BLOCKED",
      "QTEL",
      "TELAPSE",
      "UT",
      "DATAMODE",
      "BUNIT",
      "AZTEL",
      "OBJECT",
      "ORIENTAT",
      "TDISPn",
      "OBS_MODE",
      "SUNANGLE",
      "PCOUNT",
      "BITPIX",
      "DEC_PNT",
      "GROUPS",
      "AIRMASS",
      "HA",
      "TIME-OBS"
    ).map(p => HeaderConfig(p, p, p, p, p))
  }

  def readHeaderConfigFlat(): List[HeaderConfig] = {
    val path                      = getClass.getResource("/fitsHeaders_flat.conf").getPath
    val configFileContent: Config = ConfigFactory.parseFile(new File(path))

    val keywordConfigs = configFileContent.getConfigList("fits-keyword-config").asScala

    keywordConfigs.map { keywordConfig: Config =>
      val keyword      = keywordConfig.getString("keyword")
      val obsEventName = keywordConfig.getString("obs_event_name")
      val eventKey     = keywordConfig.getString("event_key")
      val paramKey     = keywordConfig.getString("param_key")
      val jsonPath     = keywordConfig.getString("json_path")
      HeaderConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
    }.toList
  }

//  def readHeaderConfigNested(): List[HeaderConfig] = {
//    val path                      = getClass.getResource("/fitsHeaders_nested.conf").getPath
//    val configFileContent: Config = ConfigFactory.parseFile(new File(path))
//
//    val keywordConfigs = configFileContent.getConfig("fits-keyword-config")
//
//    keywordConfigs
//      .entrySet()
//      .asScala
//      .map { keywordConfig =>
//        val keyword = new String(keywordConfig.getKey.getBytes(), StandardCharsets.UTF_8).split("\\.")(0)
//        val str     = keywordConfig.getValue.render()
////        val obsEventName = keywordConfig.getString.("obs_event_name")
////        val eventKey     = keywordConfig.getString("event_key")
////        val paramKey     = keywordConfig.getString("param_key")
////        val jsonPath     = keywordConfig.getString("json_path")
////        HeaderConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
//        HeaderConfig(keyword, str, str, str, str)
//      }
//      .toList
//  }

}

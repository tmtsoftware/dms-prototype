package metadata.snapshot.processor

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events._
import csw.prefix.models.Subsystem.{ESW, IRIS}
import csw.prefix.models.{Prefix, Subsystem}
import io.bullet.borer.Json
import metadata.db.ParamSetData
import metadata.snapshot.processor.HeaderConfig.{ComplexConfig, SimpleConfig}
import metadata.snapshot.processor.SnapshotProcessorUtil.readHeaderList
import nom.tam.fits.Header

import scala.collection.MapView
import scala.jdk.CollectionConverters.{CollectionHasAsScala, ConcurrentMapHasAsScala}

case class HeaderReadConfig(eventKey: EventKey, paramKey: String, obsEventName: String)

sealed trait HeaderConfig
object HeaderConfig {
  final case class ComplexConfig(keyword: String, obsEventName: String, eventKey: String, paramKey: String, jsonPath: String)
      extends HeaderConfig
  final case class SimpleConfig(keyword: String, value: String) extends HeaderConfig
}
object SnapshotProcessor extends App {
  val source: Prefix = Prefix(ESW, "filter")

  // insert 3 snapshots
  val snapshotsOfExposure =
    new ConcurrentHashMap[String, ConcurrentHashMap[EventKey, Event]]() // this can be String -> EventKey ->  paramSet

  List("startExposure", "midExposure", "endExposure").foreach(obsName =>
    snapshotsOfExposure.put(obsName, SnapshotProcessorUtil.createSnapshot(source))
  )

//  ----------------------------- process snapshot----------------------------------

  private val subsystem      = IRIS
  val keywords: List[String] = readHeaderList()(subsystem)

  (1 to 50).foreach { _ =>
    val start: Long = System.currentTimeMillis()

    val headerData = SnapshotProcessorUtil.getHeaderData(snapshotsOfExposure)

    val formattedHeaders = SnapshotProcessorUtil.generateFormattedHeader(keywords, headerData)
    println(formattedHeaders)
    println("time : " + (System.currentTimeMillis() - start))
    println(headerData.size)
  }
//  --------------------------------------------------------------------------------

}

object SnapshotProcessorUtil {

  def generateFormattedHeader(keywords: Seq[String], headerData: Map[String, Option[String]]) = {
    val fitsHeader = new Header()
    keywords.foreach { keyword =>
      headerData.get(keyword).flatten.map { fitsHeader.addValue(keyword, _, "") }
    }

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
      snapshotObsEventName: String,
      snapshot: ConcurrentHashMap[EventKey, Event]
  ): List[(String, Option[String])] = {
    val headerConfigs: List[HeaderConfig] = readHeaderConfigNested()
    val headerData: List[(String, Option[String])] = headerConfigs
      .filter {
        case ComplexConfig(_, configObsEventName, _, _, _) if snapshotObsEventName == configObsEventName => true
        case SimpleConfig(_, _) if snapshotObsEventName == "exposureStart"                               => true
        case _                                                                                           => false
      }
      .map {
        case ComplexConfig(keyword, _, eventKey, paramKey, jsonPath) =>
          val value = snapshot.asScala
            .get(EventKey(eventKey))
            .map(e => e.paramSet)
            .flatMap(_.find(p => p.keyName.equals(paramKey)))
            .map(_.head match { case s: String => s })
          (keyword, value)
        case SimpleConfig(keyword, value) => (keyword, Some(value))
      }
    headerData
  }

  private def readHeaderConfig(): MapView[String, HeaderReadConfig] = {
    val path                      = getClass.getResource("/fitsHeaders_plain.conf").getPath
    val configFileContent: Config = ConfigFactory.parseFile(new File(path))

    val configStr = configFileContent.getConfig("fits-keyword-config").root().render(ConfigRenderOptions.concise())

    val fitsHeaderConfig = Json.decode(configStr.getBytes).to[Map[String, String]].value

    fitsHeaderConfig.view.mapValues { s =>
      val (eveParam, obsEventName) = s.splitAt(s.indexOf("@"))
      val (eventKey, paramKey)     = eveParam.splitAt(eveParam.indexOf(":"))
      HeaderReadConfig(EventKey(eventKey), paramKey.drop(1), obsEventName.drop(1))
    }
  }

  def readHeaderConfigFlat(): List[ComplexConfig] = {
    val path                      = getClass.getResource("/fits-headers-flat.conf").getPath
    val configFileContent: Config = ConfigFactory.parseFile(new File(path))

    val keywordConfigs = configFileContent.getConfigList("fits-keyword-config").asScala

    keywordConfigs.map { keywordConfig: Config =>
      val keyword      = keywordConfig.getString("keyword")
      val obsEventName = keywordConfig.getString("obs-event-name")
      val eventKey     = keywordConfig.getString("event-key")
      val paramKey     = keywordConfig.getString("param-key")
      val jsonPath     = keywordConfig.getString("json-path")
      ComplexConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
    }.toList
  }

  def readHeaderConfigNested(): List[HeaderConfig] = {
    val baseConfigPath           = getClass.getResource("/base-header-mappings.conf").getPath
    val instrumentConfigPath     = getClass.getResource(s"/IRIS-header-mappings.conf").getPath
    val baseConfig: Config       = ConfigFactory.parseFile(new File(baseConfigPath))
    val instrumentConfig: Config = ConfigFactory.parseFile(new File(instrumentConfigPath)).withFallback(baseConfig).resolve()
    val keywords                 = instrumentConfig.root().keySet().asScala.toList
    keywords.map { keyword =>
      val keywordConfig: Config = instrumentConfig.getConfig(keyword)
      if (keywordConfig.hasPath("value")) {
        SimpleConfig(keyword, keywordConfig.getString("value"))
      }
      else {
        val obsEventName = keywordConfig.getString("obs-event-name")
        val eventKey     = keywordConfig.getString("event-key")
        val paramKey     = keywordConfig.getString("param-key")
        val jsonPath     = keywordConfig.getString("json-path")
        ComplexConfig(keyword, obsEventName, eventKey, paramKey, jsonPath)
      }
    }
  }

  def readHeaderList(): Map[Subsystem, List[String]] = {
    val path           = getClass.getResource("/header-list.conf").getPath
    val config: Config = ConfigFactory.parseFile(new File(path))
    val subsystemNames = config.root().keySet().asScala.toList
    subsystemNames.map { subsystemName =>
      val maybeSubsystem: Option[Subsystem] = Subsystem.values.toList.find(_.name == subsystemName)
      maybeSubsystem match {
        case Some(_) => maybeSubsystem.get -> config.getStringList(subsystemName).asScala.toList
        case None    => throw new RuntimeException(s"Invalid Subsystem Name : $subsystemName")
      }
    }.toMap
  }
}

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
import nom.tam.fits.Header

import scala.collection.MapView

case class HeaderReadConfig(eventKey: EventKey, paramKey: String, obsEventName: String)

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

    val headerData = SnapshotProcessorUtil.getHeaderData(snapshotsOfExposure)

    val formattedHeaders = SnapshotProcessorUtil.generateFormattedHeader(headerData)

    println("time : " + (System.currentTimeMillis() - start))
    println(headerData.size)
  }
//  --------------------------------------------------------------------------------

}

object SnapshotProcessorUtil {

  def generateFormattedHeader(headerData: Map[String, Option[String]]) = {
    val header = new Header()
    headerData.foreach { case (keyword, Some(data)) => header.addValue(keyword, data, "") }

    val os = new ByteArrayOutputStream();
    val ps = new PrintStream(os);

    header.dumpHeader(ps)

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
  ): Map[String, String] = {
    val headerConfig: MapView[String, HeaderReadConfig] = readHeaderConfig()
    val headerData: Map[String, String] = headerConfig.map {
      case (keyword, config) =>
        val value = snapshot(config.eventKey).paramSet
          .find(p => p.keyName.equals(config.paramKey))
          .map(_.head match { case s: String => s })
          .get
        (keyword, value)
    }.toMap
    headerData
  }

  private def readHeaderConfig(): MapView[String, HeaderReadConfig] = {
    val path                      = getClass.getResource("/fitsHeaders.conf").getPath
    val configFileContent: Config = ConfigFactory.parseFile(new File(path))

    val configStr        = configFileContent.getConfig("fits-keyword-config").root().render(ConfigRenderOptions.concise())
    val fitsHeaderConfig = Json.decode(configStr.getBytes).to[Map[String, String]].value

    fitsHeaderConfig.view.mapValues { s =>
      val (eveParam, obsEventName) = s.splitAt(s.indexOf("@"))
      val (eventKey, paramKey)     = eveParam.splitAt(eveParam.indexOf(":"))
      HeaderReadConfig(EventKey(eventKey), paramKey.drop(1), obsEventName.drop(1))
    }
  }

}

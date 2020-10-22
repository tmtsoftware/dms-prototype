package metadata.snapshot.processor

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import io.bullet.borer.Json
import nom.tam.fits.Header

case class HeaderReadConfig(eventKey: EventKey, paramKey: String, obsEventName: String)

object SnapshotProcessor extends App {
  val source      = Prefix(ESW, "filter")
  val payloadSize = 1024 * 5 // 5kb

  // insert 3 snapshots
  val snapshotsOfExposure =
    new ConcurrentHashMap[String, ConcurrentHashMap[EventKey, Event]]() // this can be String -> EventKey ->  paramSet

  List("startExposure", "midExposure", "endExposure").foreach(obsName => snapshotsOfExposure.put(obsName, createSnapshot()))

  //read config to get all fits headers
  private val path                      = getClass.getResource("/fitsHeaders.conf").getPath
  private val configFileContent: Config = ConfigFactory.parseFile(new File(path))

  val configStr                = configFileContent.getConfig("fits-keyword-config").root().render(ConfigRenderOptions.concise())
  private val fitsHeaderConfig = Json.decode(configStr.getBytes).to[Map[String, String]].value

  private val headerConfig = fitsHeaderConfig.view.mapValues { s =>
    val (eveParam, obsEventName) = s.splitAt(s.indexOf("@"))
    val (eventKey, paramKey)     = eveParam.splitAt(eveParam.indexOf(":"))
    HeaderReadConfig(EventKey(eventKey), paramKey.drop(1), obsEventName.drop(1))
  }

//  ----------------------------- process snapshot----------------------------------

  (1 to 50).foreach { _ =>
    val start: Long = System.currentTimeMillis()

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

    val header = new Header()
    headerData.foreach { case (keyword, Some(data)) => header.addValue(keyword, data, "") }

    val os = new ByteArrayOutputStream();
    val ps = new PrintStream(os);

    header.dumpHeader(ps)

    val output = os.toString("UTF8");

    println("time : " + (System.currentTimeMillis() - start))
    println(headerData.size)
  //    println("-- " + output)
  }
//  --------------------------------------------------------------------------------

  private def createSnapshot(): ConcurrentHashMap[EventKey, Event] = {
    val snapshot: ConcurrentHashMap[EventKey, Event] = new ConcurrentHashMap[EventKey, Event]()

    (1 to 2300).foreach { i =>
      val event = SystemEvent(source, EventName(s"event_key_$i"))
        .madd(StringKey.make("payloadKey").set("0" * payloadSize), StringKey.make(s"param_key_$i").set(s"param-value-$i"))

      snapshot.put(event.eventKey, event)
    }

    snapshot
  }
}

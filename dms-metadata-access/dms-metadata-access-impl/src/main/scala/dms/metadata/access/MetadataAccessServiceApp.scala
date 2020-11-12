package dms.metadata.access

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.database.DatabaseServiceFactory
import csw.event.client.EventServiceFactory
import csw.location.client.ActorSystemFactory
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.WFOS
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import org.jooq.DSLContext

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object MetadataAccessServiceApp extends App {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  import system.executionContext
  private val dslContext: DSLContext = Await.result(new DatabaseServiceFactory(system).makeDsl(), 10.seconds)

  private val headerProcessor                              = new HeaderProcessor
  private val databaseConnector                            = new DatabaseReader(dslContext)
  private val metadataAccessService: MetadataAccessService = new MetadataAccessImpl(databaseConnector, headerProcessor)

  private val eventService = new EventServiceFactory().make("localhost", 26379)
  private val subscriber   = eventService.defaultSubscriber

  val expIdKey = StringKey.make("exposureId")

  println("time taken , count of keywords")
  subscriber
    .subscribe(Set(EventKey(Prefix(WFOS, "snapshot"), EventName("snapshotComplete"))))
    .runForeach { e =>
      val exposureId: String = e.asInstanceOf[SystemEvent](expIdKey).head

      val startTime = System.currentTimeMillis()
      metadataAccessService
        .getFITSHeader(exposureId)
        .foreach(x => println(s"${System.currentTimeMillis() - startTime} , ${x.lines().count()}"))
    }

//  //TODO start http server
  //  private val header: String = Await.result(metadataAccessService.getFITSHeader("2034A-P054-O010-IRIS-BLU1-SCI1-1"), 5.seconds)
  //  println(header)

//  system.terminate()
}

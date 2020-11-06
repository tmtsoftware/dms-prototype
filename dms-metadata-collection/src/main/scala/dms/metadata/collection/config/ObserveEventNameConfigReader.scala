package dms.metadata.collection.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.events.EventName

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ObserveEventNameConfigReader {

  def read(): Set[EventName] = {
    val path              = getClass.getResource("/application.conf").getPath
    val config: Config    = ConfigFactory.parseFile(new File(path))
    val observeEventNames = config.getStringList("dms.collection.observe-event-names").asScala
    observeEventNames.map(EventName).toSet
  }
}

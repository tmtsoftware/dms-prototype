package dms.metadata.collection.config

import com.typesafe.config.{Config, ConfigFactory}
import csw.params.events.EventName

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ObserveEventNameConfigReader {

  def read(): Set[EventName] = {
    val config: Config    = ConfigFactory.load()
    val observeEventNames = config.getStringList("dms.collection.observe-event-names").asScala
    observeEventNames.map(EventName).toSet
  }
}

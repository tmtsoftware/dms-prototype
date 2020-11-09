package dms.metadata.collection.util

import csw.prefix.models.Subsystem

object SubsystemExtractor {

  def extract(expId: String): Subsystem = { //FIXME this class is duplicated in access and collection service
    expId.split("-") match {
      case Array(_, _, _, system, _, _, _) => Subsystem.withName(system)
    }
  }
}

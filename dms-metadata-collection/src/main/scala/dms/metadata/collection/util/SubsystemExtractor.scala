package dms.metadata.collection.util

import csw.prefix.models.Subsystem

object SubsystemExtractor {

  def extract(expId: String): Subsystem = { //FIXME this class is duplicated in access and collection service
    expId.split("-") match {
      case Array(_, _, _, subSystem, _, _, _) =>
        val maybeSubsystem = Subsystem.withNameInsensitiveOption(subSystem)
        maybeSubsystem match {
          case Some(foundSubsystem) => foundSubsystem
          case None                 => throw new RuntimeException(s"Invalid subsystem $subSystem present in for Exposure id : $expId ")
        }
      case _ => throw new RuntimeException(s"Invalid format for Exposure id : $expId ")
    }
  }
}

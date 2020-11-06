package dms.metadata.collection.util

import csw.prefix.models.Subsystem

import scala.util.matching.Regex

object SubsystemExtractor {

  def extract(expId: String): Subsystem = {
    val pattern: Regex = "(.*)-(.*)-(.*)-(.*)-(.*)-(.*)-(.*)".r

    val subsystem: Subsystem = expId match {
      case pattern(_, _, _, subsystem, _, _, _) =>
        val maybeSubsystem = Subsystem.values.find(p => p.name == subsystem)
        maybeSubsystem match {
          case Some(value) => value
          case None        => throw new RuntimeException(s"Invalid subsystem $subsystem present in for Exposure id : $expId ")
        }
      case _ => throw new RuntimeException(s"Invalid format for Exposure id : $expId ")
    }
    subsystem
  }
}

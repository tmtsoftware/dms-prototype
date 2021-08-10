package dms.metadata.collection.cli

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

import java.nio.file.Path

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("starts metadata collection job")
  final case class Start(
      @HelpMessage(
        "Path of collection service confs -m /path/to/WFOS-keyword-mappings.conf -m /path/to/IRIS-keyword-mappings.conf ..."
      )
      @Short("m")
      keywordMappingsConf: List[Path] = Nil
  ) extends Command

  object Start {
    def apply(
        keywordMappingsConf: List[Path] = Nil
    ): Start = {
      new Start(keywordMappingsConf)
    }
  }

}

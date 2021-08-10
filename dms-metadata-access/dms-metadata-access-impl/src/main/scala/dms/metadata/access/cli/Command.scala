package dms.metadata.access.cli

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

import java.nio.file.Path

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("starts metadata access service")
  final case class Start(
      @Short("p")
      @HelpMessage(
        "Access Service HTTP server will be bound to this port. " +
          "If a value is not provided, random port will be used"
      )
      port: Option[Int],
      @Short("k")
      @HelpMessage("Path of access service /path/to/header-keywords.conf")
      keywordsConf: Option[Path]
  ) extends Command

  object Start {
    def apply(
        port: Option[Int],
        keywordsConf: Option[Path]
    ): Start = {
      new Start(port, keywordsConf)
    }
  }
}

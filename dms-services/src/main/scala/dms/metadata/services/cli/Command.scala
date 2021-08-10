package dms.metadata.services.cli

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

import java.nio.file.Path

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("starts both the DMS services: metadata collection and metadata access by default if no other option is provided")
  final case class Start(
      @HelpMessage("start metadata access server")
      @Short("a")
      access: Boolean = false,
      @HelpMessage("start metadata collection server")
      @Short("c")
      collection: Boolean = false,
      @HelpMessage("initialize database: setup user and tables")
      @Short("i")
      init: Boolean = false,
      @HelpMessage(
        "Access Service HTTP server will be bound to this port. " +
          "If a value is not provided, random port will be used"
      )
      @Short("p")
      port: Option[Int],
      @HelpMessage("Path of access service header-keywords.conf")
      @Short("k")
      keywordsConf: Option[Path],
      @HelpMessage(
        "Path of collection service -m /path/to/WFOS-keyword-mappings.conf -m /path/to/IRIS-keyword-mappings.conf ..."
      )
      @Short("m")
      keywordMappingsConf: List[Path] = Nil
  ) extends Command

  object Start {
    def apply(
        access: Boolean = false,
        collection: Boolean = false,
        init: Boolean = false,
        port: Option[Int],
        keywordsConf: Option[Path],
        keywordMappingsConf: List[Path] = Nil
    ): Start =
      // mark access and collection flags=true when no option is provided to start command
      if (access || collection)
        new Start(access, collection, init, port, keywordsConf, keywordMappingsConf)
      else new Start(true, true, init, port, keywordsConf, keywordMappingsConf)
  }

}

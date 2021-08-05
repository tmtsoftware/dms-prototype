package dms.metadata.services

import caseapp.{CommandName, HelpMessage, ExtraName => Short}
import csw.network.utils.SocketUtils

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
      port: Int = SocketUtils.getFreePort
  ) extends Command

  object Start {
    def apply(
        access: Boolean = false,
        collection: Boolean = false,
        init: Boolean = false,
        port: Int
    ): Start =
      // mark access and collection flags=true when no option is provided to start command
      if (access || collection)
        new Start(access, collection, init, port)
      else new Start(true, true, init, port)
  }

}

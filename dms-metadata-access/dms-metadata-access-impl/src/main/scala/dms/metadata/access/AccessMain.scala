package dms.metadata.access

import caseapp.RemainingArgs
import caseapp.core.app.CommandApp
import dms.metadata.access.cli.Command
import dms.metadata.access.cli.Command.Start

import java.nio.file.Path

object AccessMain extends CommandApp[Command] {

  override def run(command: Command, remainingArgs: RemainingArgs): Unit = {
    command match {
      case Start(port, keywordsConf) =>
        start(port, keywordsConf)
    }
  }

  def start(
      port: Option[Int],
      keywordsConf: Option[Path]
  ): AccessWiring = {
    val wiring = new AccessWiring(port, keywordsConf)
    wiring.start()
    wiring
  }

}

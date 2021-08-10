package dms.metadata.collection

import caseapp.{CommandApp, RemainingArgs}
import dms.metadata.collection.cli.Command
import dms.metadata.collection.cli.Command.Start

import java.nio.file.Path

object CollectionMain extends CommandApp[Command] {

  override def run(command: Command, remainingArgs: RemainingArgs): Unit = {
    command match {
      case Start(keywordMappingsConf) =>
        start(keywordMappingsConf)
    }
  }

  def start(
      keywordMappingsConf: List[Path]
  ): CollectionWiring = {
    val wiring = new CollectionWiring(keywordMappingsConf)
    wiring.start()
    wiring
  }

}

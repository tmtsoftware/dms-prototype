package exp.writer

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.UUID

import akka.actor.typed.{ActorSystem, DispatcherSelector}
import exp.api.SystemEventRecord
import io.bullet.borer.Json

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.IterableHasAsJava

class JsonIO(path: String)(implicit actorSystem: ActorSystem[_]) {

  private val blockingEC: ExecutionContextExecutor = actorSystem.dispatchers.lookup(DispatcherSelector.blocking())

  private val targetDir: Path = Paths.get(path)
  private val tmpDir: Path    = Paths.get("/tmp/json")

  Files.createDirectories(targetDir)
  Files.createDirectories(tmpDir)

  def writeJson(batch: Seq[SystemEventRecord]): Future[Path] =
    Future {
      val uuid          = UUID.randomUUID().toString
      val tmpLocation   = tmpDir.resolve(s"$uuid.json")
      val finalLocation = targetDir.resolve(s"$uuid.json")
      val lines         = batch.map(x => Json.encode(x).toUtf8String).asJava
      Files.write(tmpLocation, lines)
      Files.move(tmpLocation, finalLocation, StandardCopyOption.ATOMIC_MOVE)
    }(blockingEC)

}

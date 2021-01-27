package eng.arch.ingestor.util

import java.io.{BufferedOutputStream, FileOutputStream, OutputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.UUID
import java.util.zip.GZIPOutputStream

import akka.actor.typed.{ActorSystem, DispatcherSelector}
import csw.params.core.formats.ParamCodecs._
import csw.params.events.Event
import io.bullet.borer.Json

import scala.concurrent.{ExecutionContextExecutor, Future}

class JsonIO(path: String)(implicit actorSystem: ActorSystem[_]) {

  private val blockingEC: ExecutionContextExecutor = actorSystem.dispatchers.lookup(DispatcherSelector.blocking())

  private val targetDir: Path = Paths.get(path)
  private val tmpDir: Path    = Paths.get("/tmp/json")

  Files.createDirectories(targetDir)
  Files.createDirectories(tmpDir)

  def write(batch: Seq[Event]): Future[Path] =
    Future {
      val uuid             = UUID.randomUUID().toString
      val tmpLocation      = tmpDir.resolve(s"$uuid.json.gz")
      val finalLocation    = targetDir.resolve(s"$uuid.json.gz")
      val os: OutputStream = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tmpLocation.toFile)))
      Json.encode(batch).to(os).result.close()
      Files.move(tmpLocation, finalLocation, StandardCopyOption.ATOMIC_MOVE)
    }(blockingEC)

}

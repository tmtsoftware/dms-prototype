package dms.metadata.access

import akka.actor.typed.ActorSystem
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.IRIS

import scala.concurrent.Future

class MetadataAccessService(databaseConnector: DatabaseReader, headerProcessor: HeaderProcessor) {

  def getFITSHeader(expId: String)(implicit system: ActorSystem[_]): Future[String] = {
    val headerKeywords: Map[Subsystem, List[String]] = headerProcessor.loadHeaderList()
    getFITSHeader(expId, headerKeywords(IRIS)) //FIXME extract instrument name from expId
  }

  def getFITSHeader(expId: String, keywords: List[String])(implicit system: ActorSystem[_]): Future[String] = {
    import system.executionContext

    val keywordData: Future[Map[String, String]] = databaseConnector.readKeywordData(expId)

    val headerString: Future[String] = keywordData.map {
      headerProcessor.generateFormattedHeader(keywords, _)
    }
    headerString
  }
}

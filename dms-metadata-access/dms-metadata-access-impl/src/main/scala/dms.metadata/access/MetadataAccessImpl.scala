package dms.metadata.access

import akka.actor.typed.ActorSystem
import csw.prefix.models.Subsystem
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import dms.metadata.access.util.SubsystemExtractor

import scala.concurrent.Future

class MetadataAccessImpl(databaseConnector: DatabaseReader, headerProcessor: HeaderProcessor)(implicit system: ActorSystem[_])
    extends MetadataAccessService {

  override def getFITSHeader(expId: String): Future[String] = {

    val headerKeywords: Map[Subsystem, List[String]] = headerProcessor.loadHeaderList()
    getFITSHeader(expId, headerKeywords(SubsystemExtractor.extract(expId)))
  }

  override def getFITSHeader(expId: String, keywords: List[String]): Future[String] = {
    import system.executionContext

    val keywordData: Future[Map[String, String]] = databaseConnector.readKeywordData(expId)

    val headerString: Future[String] = keywordData.map {
      headerProcessor.generateFormattedHeader(keywords, _)
    }
    headerString
  }
}

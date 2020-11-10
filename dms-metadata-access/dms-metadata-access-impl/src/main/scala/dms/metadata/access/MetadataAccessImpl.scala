package dms.metadata.access

import akka.actor.typed.ActorSystem
import dms.metadata.access.core.{DatabaseReader, HeaderProcessor}
import dms.metadata.access.util.SubsystemExtractor

import scala.concurrent.Future

class MetadataAccessImpl(databaseConnector: DatabaseReader, headerProcessor: HeaderProcessor)(implicit system: ActorSystem[_])
    extends MetadataAccessService {
  import system.executionContext
  private val headerKeywords = headerProcessor.loadHeaderList()

  override def getFITSHeader(expId: String): Future[String] =
    getFITSHeader(expId, headerKeywords(SubsystemExtractor.extract(expId)))

  override def getFITSHeader(expId: String, keywords: List[String]): Future[String] = {
    val keywordData = databaseConnector.readKeywordData(expId)
    keywordData.map(headerProcessor.generateFormattedHeader(keywords, _))
  }
}

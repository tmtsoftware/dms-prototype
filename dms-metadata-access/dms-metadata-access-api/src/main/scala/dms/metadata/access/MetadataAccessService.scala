package dms.metadata.access

import scala.concurrent.Future

trait MetadataAccessService {

  def getFITSHeader(expId: String): Future[String]
  def getFITSHeader(expId: String, keywords: List[String]): Future[String]
}

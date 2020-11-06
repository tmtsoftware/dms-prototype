package dms.metadata.access.core

import akka.actor.typed.ActorSystem
import csw.database.scaladsl.JooqExtentions.RichResultQuery
import org.jooq.DSLContext

import scala.concurrent.Future

class DatabaseReader(dslContext: DSLContext) {

  def readKeywordData(expId: String)(implicit system: ActorSystem[_]): Future[Map[String, String]] = {
    import system.executionContext

    val getDatabaseQuery =
      dslContext.resultQuery(s"select * from keyword_values where exposure_id= ?", expId)

    getDatabaseQuery.fetchAsyncScala[(String, String, String)].map { _.map(row => row._2 -> row._3).toMap }
    //FIXME handle failures
  }

}

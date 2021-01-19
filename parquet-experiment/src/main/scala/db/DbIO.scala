package db

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import exp.api.{SystemEventRecord, SystemEventRecord2}
import scalikejdbc._

import scala.concurrent.{Future, blocking}

class DbIO(implicit session: AutoSession, actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def setup(): Boolean = {
    sql"""
         |DROP TABLE IF EXISTS events;
         |
         |create table events
         |(
         |    exposureid   varchar(50),
         |    obseventname varchar(50),
         |    eventid      varchar(50),
         |    source       varchar(50),
         |    eventname    varchar(50),
         |    eventtime    varchar(50),
         |    seconds      bigint,
         |    nanos        bigint,
         |    paramset     bytea
         |);
         |
         |""".stripMargin.execute()()
  }

  def write(records: Seq[SystemEventRecord2]): Future[Done] = {
    Source(records).mapAsync(1)(write).run()
  }

  def write(record: SystemEventRecord2): Future[Int] = {
    import record._
    Future {
      blocking {
        sql"""
             |insert into events (
             |  exposureid,
             |  obseventname,
             |  eventid,
             |  source,
             |  eventname,
             |  eventtime,
             |  seconds,
             |  nanos,
             |  paramset
             |) values (
             |  $exposureId,
             |  $obsEventName,
             |  $eventId,
             |  $source,
             |  $eventName,
             |  $eventTime,
             |  $seconds,
             |  $nanos,
             |  $paramSet
             |)
       |""".stripMargin.update()()
      }
    }
  }

  def batchWrite(records: Seq[SystemEventRecord2]): Unit = {
    val entries: Seq[Seq[Any]] = {
      records.map(r =>
        Seq(r.exposureId, r.obsEventName, r.eventId, r.source, r.eventName, r.eventTime, r.seconds, r.nanos, r.paramSet)
      )
    }
    sql"""
       |insert into events (
       |  exposureid,
       |  obseventname,
       |  eventid,
       |  source,
       |  eventname,
       |  eventtime,
       |  seconds,
       |  nanos,
       |  paramset
       |) values (
       |  ?,
       |  ?,
       |  ?,
       |  ?,
       |  ?,
       |  ?,
       |  ?,
       |  ?,
       |  ?
       |)
       |""".stripMargin.batch(entries: _*)()
  }

  def read(exposureId: String)(implicit session: AutoSession): List[Array[Byte]] = {
    val query = sql"select paramset from events where exposureid = $exposureId"
    query.map(_.bytes("paramset")).list()()
  }

}

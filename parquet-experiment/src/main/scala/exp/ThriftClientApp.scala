package exp

import scalikejdbc.{AutoSession, ConnectionPool, scalikejdbcSQLInterpolationImplicitDef}

object ThriftClientApp extends App {

  Class.forName("org.apache.hive.jdbc.HiveDriver")
  ConnectionPool.singleton("jdbc:hive2://192.168.1.5:10000", "", "")

  implicit val a = AutoSession

  sql"select * from json.`/Users/in-shubham.jaybhaye/projects/tmt/dms-prototype/target/data/json` limit 5".foreach(x =>
    println(x.string("eventName"))
  )

}

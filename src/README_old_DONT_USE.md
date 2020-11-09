### Setup

#### Start redis

```
cs launch csw-services:0.1.0-SNAPSHOT -- start -e
```

#### Start postgres

##### vi /usr/local/var/postgres/postgresql.conf
 Add this :
 listen_addresses = '*'
 
##### vi /usr/local/var/postgres/pg_hba.conf
 Add this :
 host    all         all         <your_Ip>/24    trust

NOTE: This will enable you to connect postgres using IP. As our CSW Database Service resolves DB location from Location Service, this is needed.

##### Start Db server using command:

postgres

NOTE : You should see logs like, listening on IPv4 address "0.0.0.0", port 5432 - 

```
2020-10-17 21:51:30.071 IST [44009] LOG:  starting PostgreSQL 12.4 on x86_64-apple-darwin19.5.0, compiled by Apple clang version 11.0.3 (clang-1103.0.32.62), 64-bit
2020-10-17 21:51:30.073 IST [44009] LOG:  listening on IPv6 address "::", port 5432
2020-10-17 21:51:30.073 IST [44009] LOG:  listening on IPv4 address "0.0.0.0", port 5432
2020-10-17 21:51:30.073 IST [44009] LOG:  listening on Unix socket "/tmp/.s.PGSQL.5432"
2020-10-17 21:51:30.087 IST [44010] LOG:  database system was shut down at 2020-10-17 21:47:33 IST
2020-10-17 21:51:30.093 IST [44009] LOG:  database system is ready to accept connections
```

#### Create DB,User,Table and Permissions

##### connect to db using psql and default DB postgres

```
psql -d postgres -h <your_Ip> -p 5432
```

##### Create DB,User,Table and Permissions

```
create database mydb;

----OUTPUT----- CREATE DATABASE

\c mydb;

----OUTPUT----- You are now connected to database "mydb" as user "<your_name>".

create user myuser with encrypted password 'mypass';

----OUTPUT----- CREATE ROLE

grant all privileges on database mydb to myuser;

----OUTPUT----- GRANT

CREATE TABLE event_snapshots (
  exposure_id VARCHAR(50) NOT NULL,
  obs_event_name VARCHAR(50) NOT NULL,
  source VARCHAR(50) NOT NULL,
  eventName VARCHAR(50) NOT NULL,
  eventId VARCHAR(50) NOT NULL,
  eventTime TIMESTAMP NOT NULL,
  paramSet Json
);

----OUTPUT----- CREATE TABLE

GRANT ALL PRIVILEGES ON TABLE event_snapshots TO myuser;

----OUTPUT----- GRANT
```

#### Register DB service to location service by executing below command in .http file
```
POST http://localhost:7654/post-endpoint
Content-Type: application/json

{
  "_type" : "Register",
  "registration" : {
    "_type" : "TcpRegistration",
    "connection" : {
      "prefix" : "CSW.DatabaseServer",
      "componentType" : "Service",
      "connectionType" : "tcp"
    },
    "port" : 5432,
    "path" : "",
    "networkType" : {
      "_type" : "Private"
    },
    "metadata" : { }
  }
}
```

### To Play around Db insertion refer

DbTestAppFlattenedEvents.scala

DbTestAppSingleEvent.scala

### To take snapshots and persist to db
Edit intelliJ run config of MinimalPSubscribeSampler
 
SET ENV : 

DB_READ_USERNAME=myuser;DB_READ_PASSWORD=mypass

Start sampler :

MinimalPSubscribeSampler.scala

### Start Publisher

PublisherAppWithPerfLikeSetup.scala


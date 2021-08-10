# DMS-prototype - TMT Data Management System
This repository begins the implementation of the TMT Data Management System (DMS). This repository contains protyping code that addresses risks during the DMS final design phase. A different repository will be used for the full development phase in the future.

## Quick Setup:

#### Pre-requisite

Postgres should be installed on your machine with `postgres` database, for more details refer [csw docs](https://tmtsoftware.github.io/csw//services/database.html)

#### Start required csw services ( Location Service, Event Service and Database Service)

```bash
cs launch csw-services:4.0.0-M1 -- start -e -d
```

#### Start DMS Metadata Access Service and Collection Job

```bash
cs launch dms-services:commitSHA -- start -p 9999
```

* Additionally, if required, add `-i` argument to initialize database 

##### To pass external configurations use `--keywords-conf` and `--keyword-mappings-conf` argument.

* Argument `--keyword-mappings-conf` should be repeated to pass mappings for each subsystem separately and should follow file name convention like `{subsystem}-keyword-mappings.conf`. e.g. `IRIS-keyword-mappings.conf`

```bash
cs launch dms-services:commitSHA -- start -p 9999 --keywords-conf "/path/to/header-keywords.conf" --keyword-mappings-conf "/path/to/IRIS-keyword-mappings.conf" --keyword-mappings-conf "/path/to/WFOS-keyword-mappings.conf"
```

These can be standalone config files or composed config files written in [HOCON](https://github.com/lightbend/config#using-hocon-the-json-superset) format, for more details around what configurations are expected in these files refer existing access service [resource](https://github.com/tmtsoftware/dms-prototype/tree/main/dms-metadata-access/dms-metadata-access-impl/src/main/resources) files and collection service [resource](https://github.com/tmtsoftware/dms-prototype/tree/main/dms-metadata-collection/src/main/resources) files.


---

## Detailed Manual Setup:

##### Start Event Service and Database Service
cs launch csw-services:4.0.0-M1 -- start -e -d

##### Start postgres db server

User should have `postgres` server installed with `postgres` database and `dmsuser` user created.

##### Login to postgres from command line
``` 
psql -d postgres -h localhost -p 5432 -U dmsuser
```

##### create snapshots table and index 
```
create table event_snapshots
(
    exposure_id    varchar(50) not null,
    obs_event_name varchar(50) not null,
    source         varchar(50) not null,
    eventname      varchar(50) not null,
    eventid        varchar(50) not null,
    eventtime      timestamp   not null,
    paramset       text
);


create index idx_exposure_id
    on event_snapshots (exposure_id);
```

##### create Keywords table and index

```
create table keyword_values
(
    exposure_id varchar(50) not null,
    keyword     varchar(50) not null,
    value       varchar(50) not null
);

create index idx_hdr_exposure_id
    on keyword_values (exposure_id);
```

#### Running Code:

##### Start Observe and other event Publisher

`src/test/scala/simulator/PublisherAppWithPerfLikeSetup.scala`

##### Start collection service:

`src/main/scala/dms/metadata/collection/Main.scala`

Note: You should see some data populated in tables : `keyword_values` and `event_snapshots`

##### Run access service:

`src/main/scala/dms/metadata/access/HttpServer.scala`

#### Test Api

Copy any `exposureId` from table : `keyword_values`, update apptest.http with ip, port and exp-id to do a get call

# DMS-prototype - TMT Data Management System
This repository begins the implementation of the TMT Data Management System (DMS). This repository contains protyping code that addresses risks during the DMS final design phase. A different repository will be used for the full development phase in the future.



#### SETUP:

##### Start redis
cs launch csw-services:0.1.0-SNAPSHOT -- start -e

##### Start postgres db server

User should have `postgres` server installed with `postgres` database and `postgres` user created.

##### Login to postgres from command line
``` 
psql -d postgres -h localhost -p 5432 -U postgres
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

Note: Copy any `exposureId` from table : `keyword_values` and run Main with that `exposureId`

`src/main/scala/dms/metadata/access/Main.scala`

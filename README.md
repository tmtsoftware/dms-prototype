# DMS-prototype - TMT Data Management System
This repository begins the implementation of the TMT Data Management System (DMS). This repository contains protyping code that addresses risks during the DMS final design phase. A different repository will be used for the full development phase in the future.

-----------------------

#### Requirements:

##### Start redis

```
cs launch csw-services:0.1.0-SNAPSHOT -- start -e
```

##### Start postgres db server

User should have `postgres` server installed with
`postgres` database and `postgres` user created.

-----------------------

#### Running snapshotting Sampler:

There are 3 types of samplers :

- Get sampler
- Get Sampler with static list of keys
- Psubscribe sampler

1. Run Publisher, which publishes `observer` event:
`src/main/scala/metadata/PublisherApp.scala`

2. Run any sampler from :
 `src/main/scala/metadata/samplers`

NOTE : For extensive perf testing , run modelObs perf setup along with this to have prod like scenario where there are many publishers and subscribers.
 
 -----------------------
 
#### Running Db Sampler:
 It persist both snapshots and keywords : 

1. Run 

  `src/main/scala/metadata/db/PersistHeaderKeywords.scala`
 
 



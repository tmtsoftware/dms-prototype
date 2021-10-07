```puml
participant "Event\nService" as event
participant IRIS
database "Transfer\nArea" as transfer
participant "Metadata\nCollection\nService" as MCS
database "Metadata\nDatabase" as MDB
participant "Metadata\nAccess\nService" as MAS
participant "Detector Data\nCopy Service" as DDCS
participant "Science Data\nStorage Service" as SDSS
database "Summit\nPermanent\nStore" as store
participant "Science Data\nAccess Service" as SDAS
participant "Dataset\nInformation\nService" as DIS
participant "Migration\nService" as MS
database "HQ Store" as hq
database "CAOM Database" as caom
database "Cloud\nStorage" as cloud

IRIS -> event : publish observe event
event -> MCS : receieve observe event
event -> MCS : collect metadata
MCS -> MDB : write snapshot
event -> DIS: receive observe event
DIS -> DIS: create entry for new exposure
IRIS -> IRIS: exposure taken
IRIS -> event : publish observe event
event -> MCS : receieve observe event
event -> MCS : collect metadata
MCS -> MDB : write snapshot
IRIS -> transfer : writes data w/\nminimal header
DDCS -> MAS : query for metadata
MAS -> MDB : retrieve snapshot
MAS -> MAS: create FITS header
MAS -> DDCS : return FITS header
DDCS -> transfer : reads data
DDCS -> DDCS : insert FITS header
DDCS -> SDSS : get location
DDCS -> SDSS: writes data w/\nfull header to location
SDSS -> store: writes data
DDCS -> event: publish observe event
event -> DIS: receive observe event
DIS -> DIS: update file location
DIS -> MS: sends list of new file(s)\nto migrate plus\nCAOM info for file(s)
MS -> SDAS: requests new file(s)
SDAS -> MS: sends new file(s)
MS -> hq: stores new file(s)
hq -> MS: returns new file location(s)
MS -> DIS : notifies to update\nnew files list
MS -> caom: updates with CAOM models for new file(s)
MS -> cloud: copies file(s)
cloud -> MS : returns file location(s)
MS -> caom: updates CAOM models with cloud file location(s)
```

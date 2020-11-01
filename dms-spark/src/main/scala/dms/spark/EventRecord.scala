package dms.spark

import java.sql.Timestamp

case class EventRecord(
    exposure_id: String,
    obs_event_name: String,
    source: String,
    eventname: String,
    eventid: String,
    eventtime: Timestamp,
    paramset: String
)

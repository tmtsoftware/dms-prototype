package metadata2

import java.time.LocalDateTime

case class SnapshotRow(
    exposure_id: String,
    obs_event_name: String,
    source: String,
    eventName: String,
    eventId: String,
    eventTime: LocalDateTime,
    paramSet: String
)

case class SnapshotRow2(
    exposure_id: String,
    obs_event_name: String,
    source: String,
    eventName: String,
    eventId: String,
    eventTime: LocalDateTime,
    paramSet: Array[Byte]
)

package exp.api

case class SystemEventRecord2(
    source: String,
    eventId: String,
    eventName: String,
    eventTime: String,
    paramSet: String,
    date: String,
    hour: String,
    minute: String
)

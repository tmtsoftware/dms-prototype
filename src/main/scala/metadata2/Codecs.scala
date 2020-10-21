package metadata2

import java.time.LocalDateTime

import skunk.{Codec, Decoder}
import skunk.codec.all._

object Codecs {
  type EventCodec = Codec[((((((String, String), String), String), String), LocalDateTime), String)]
  val eventCodec: EventCodec            = (((((varchar(50) ~ varchar(50)) ~ varchar(50)) ~ varchar(50)) ~ varchar(50)) ~ timestamp) ~ varchar
  val snapshotRow: Decoder[SnapshotRow] = eventCodec.gmap[SnapshotRow]
}

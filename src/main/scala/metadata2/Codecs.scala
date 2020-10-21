package metadata2

import java.time.LocalDateTime

import csw.params.core.generics.Parameter
import io.bullet.borer.Cbor
import skunk.{Codec, Decoder}
import skunk.codec.all._
import csw.params.core.formats.ParamCodecs._

object Codecs {
  private val eventWithoutParamSet = ((((varchar(50) ~ varchar(50)) ~ varchar(50)) ~ varchar(50)) ~ varchar(50)) ~ timestamp

  type EventCodec = Codec[((((((String, String), String), String), String), LocalDateTime), String)]
  val eventCodec: EventCodec            = eventWithoutParamSet ~ varchar
  val snapshotRow: Decoder[SnapshotRow] = eventCodec.gmap[SnapshotRow]

  // bytes
  type EventCodec2 = Codec[((((((String, String), String), String), String), LocalDateTime), Array[Byte])]
  val eventCodec2: EventCodec2                    = eventWithoutParamSet ~ bytea
  val snapshotRow2: Decoder[SnapshotRow2]         = eventCodec2.gmap[SnapshotRow2]
  val paramSetDecoder: Decoder[Set[Parameter[_]]] = bytea.map(bytes => Cbor.decode(bytes).to[Set[Parameter[_]]].value)
}

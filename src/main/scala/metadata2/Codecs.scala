package metadata2

import csw.params.core.formats.ParamCodecs._
import csw.params.core.generics.Parameter
import io.bullet.borer.Cbor
import skunk.Decoder
import skunk.codec.all._

object Codecs {
  private val eventWithoutParamSet = varchar(50) ~ varchar(50) ~ varchar(50) ~ varchar(50) ~ varchar(50) ~ timestamp

  val eventCodec                        = eventWithoutParamSet ~ varchar
  val snapshotRow: Decoder[SnapshotRow] = eventCodec.gmap[SnapshotRow]

  // bytes
  val eventCodec2                                 = eventWithoutParamSet ~ bytea
  val snapshotRow2: Decoder[SnapshotRow2]         = eventCodec2.gmap[SnapshotRow2]
  val paramSetDecoder: Decoder[Set[Parameter[_]]] = bytea.map(bytes => Cbor.decode(bytes).to[Set[Parameter[_]]].value)
}

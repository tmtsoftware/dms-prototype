package eng.arch.ingestor.job.util

import java.nio.ByteBuffer

import csw.params.events.{Event, EventKey}
import io.bullet.borer.Cbor.DecodingConfig
import io.bullet.borer.{Cbor, Input, Output}
import romaine.codec.RomaineCodec

import scala.util.control.NonFatal

//TODO: this file is copied from CSW event module
object EventConverter {
  import csw.params.core.formats.ParamCodecs._

  def toEvent[Chunk: Input.Provider](bytes: Chunk): Event =
    try {
      Cbor.decode(bytes).withConfig(DecodingConfig(readDoubleAlsoAsFloat = true)).to[Event].value
    } catch { case NonFatal(_) => Event.badEvent() }

  def toBytes[Chunk: Output.ToTypeProvider](event: Event): Chunk = Cbor.encode(event).to[Chunk].result
}

object RomaineCodecs {

  implicit val eventKeyRomaineCodec: RomaineCodec[EventKey] =
    RomaineCodec.stringCodec.bimap(_.key, EventKey.apply)

  implicit val eventRomaineCodec: RomaineCodec[Event] =
    RomaineCodec.byteBufferCodec.bimap[Event](EventConverter.toBytes[ByteBuffer], EventConverter.toEvent)
}

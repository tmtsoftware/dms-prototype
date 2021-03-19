package eng.arch.ingestor.job.util

import csw.params.core.generics.KeyType._
import csw.params.core.generics.Parameter

import scala.util.Random

object ProdLikeParamSet {
  private def p1 = IntKey.make("intensity").set(Random.between(60, 70), Random.between(70, 80))
  private def p2 =
    ByteKey.make("moisture").setAll(Array[Byte](Random.between(0, 10).toByte, Random.between(20, 30).toByte))
  private def p3 = IntKey.make("elevation").set(Random.between(60, 70), Random.between(70, 80))
  private def p4 =
    ShortKey.make("resolution").setAll(Array[Short](Random.between(20, 30).toShort, Random.between(40, 50).toShort))
  private def p5 =
    LongKey.make("altitude").setAll(Array[Long](Random.between(40, 50).toLong, Random.between(60, 70).toLong))
  private def p6 = IntKey.make("temperature").set(Random.between(60, 70), Random.between(70, 80))
  private def p7 =
    FloatKey
      .make("luminescence")
      .setAll(Array[Float](Random.between(80, 90).toFloat, Random.between(100, 110).toFloat))
  private def p8 =
    DoubleKey.make("power").setAll(Array[Double](Random.between(100, 110).toFloat, Random.between(120, 130)))
  private def p9 = StringKey.make("state").set(Random.alphanumeric.take(Random.between(2, 10)).mkString)

  def paramSet: Set[Parameter[_]] =
    Set(
      p1,
      p2,
      p3,
      p4,
      p5,
      p6,
      p7,
      p8,
      p9
    )
}

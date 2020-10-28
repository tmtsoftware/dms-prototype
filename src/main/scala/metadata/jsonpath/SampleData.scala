package metadata.jsonpath

import csw.params.core.generics.KeyType.{AltAzCoordKey, IntKey, StructKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.Coords.{AltAzCoord, BASE}
import csw.params.core.models.{Angle, Struct}

object SampleData {
  // Struct1/Struct11/Struct111/Azimuth.az
  // Struct1/Struct11/Struct111/RaDec
  // Struct1/Struct12/RaDec2
  // RaDec3
  val paramSet: Set[Parameter[_]] = Set(
    StructKey
      .make("Struct1")
      .set(
        Struct().add(
          StructKey
            .make("Struct11")
            .set(
              Struct().add(
                StructKey
                  .make("Struct111")
                  .set(
                    Struct().madd(
                      AltAzCoordKey.make("Azimuth").set(AltAzCoord(BASE, Angle(25), Angle(30))),
                      IntKey.make("RaDec").set(1, 2)
                    )
                  )
              )
            )
        ),
        Struct().add(
          StructKey
            .make("Struct12")
            .set(
              Struct().add(
                IntKey.make("RaDec2").set(3, 4)
              )
            )
        )
      ),
    IntKey.make("RaDec3").set(5, 6)
  )

}

import sbt.{Def, _}
object Dependencies {

  val MetadataCollection: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-database`,
      Csw.`csw-location-client`,
      Csw.`csw-event-client`
    )
  )

  val MetadataAccessImpl: Def.Initialize[Seq[ModuleID]] = Def.setting(
    Seq(
      Csw.`csw-database`,
      Csw.`csw-location-client`,
      Libs.`nom-tam-fits`,
      Csw.`csw-event-client` //  fixme: remove  this after perf testing
    )
  )
}

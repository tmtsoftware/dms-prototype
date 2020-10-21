package metadata2

import csw.params.core.generics.KeyType.StringKey
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix

object SampleData {

  private def event(id: Int) = SystemEvent(Prefix("iris.filter"), EventName(s"move-$id"))
  private val events         = (1 to 2300).map(id => addPayload(event(id), 1024 * 5)).toList

  def addPayload(event: SystemEvent, size: Int): SystemEvent = {
    val payload = StringKey.make("payloadKey").set("0" * size)
    event.add(payload)
  }

  def snapshot: List[SnapshotRow]      = Snapshot.create("1", "expStart", events)
  val snapshotConst: List[SnapshotRow] = Snapshot.create("1", "expStart", events)
}

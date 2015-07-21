package shipreq.webapp.base.protocol

import boopickle._
import shipreq.base.util._
import shipreq.webapp.base.data.RevRange
import shipreq.webapp.base.delta._
import BoopickleMacros._
import BinCodecGeneric._
import BinCodecData.pickleRevRange

object BinCodecDelta {

  implicit final val picklePartition = pickleEnum(Partition.values)

  implicit object PickleRemoteDeltaPR extends Pickler[RemoteDeltaPR] {
    override def pickle(r: RemoteDeltaPR)(implicit state: PickleState): Unit = {
      import r.delta.partition.{pi, pd}
      state pickle r.partition
      state pickle r.revRange
      state pickle r.delta.delete
      state pickle r.delta.update
    }
    override def unpickle(implicit state: UnpickleState): RemoteDeltaPR = {
      val p = state.unpickle[Partition]
      val r = state.unpickle[RevRange]

      val pi: Pickler[Set[p.Id]]    = iterablePickler(p.pi, implicitly)
      val pd: Pickler[List[p.Data]] = iterablePickler(p.pd, implicitly)
      val x = state.unpickle(pi)
      val y = state.unpickle(pd)

      RemoteDeltaPR(p, r)(x, y)(UnivEq.force)
    }
  }

  implicit final val pickleRemoteDelta = pickleIMap(RemoteDelta.empty)
}
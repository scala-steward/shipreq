package shipreq.webapp.client.delta

import scala.annotation.tailrec
import shipreq.base.util.UnivEq
import shipreq.webapp.base.delta.Partition

case class LocalDeltaP[P <: Partition](del: Set[P#Id],
                                       upd: List[P#Data]) {

  def deltaG(p: P): LocalDeltaG = new LocalDeltaR(p, this)
}

private[delta] class LocalDeltaR[_P <: Partition](_p: _P, d: LocalDeltaP[_P]) extends LocalDeltaG {
  override type P = _P
  override def p = _p
  override def deltaP = d
}

// TODO LocalDeltaG's dep types aren't done properly
trait LocalDeltaG {
  type P <: Partition
  def p: P
  def deltaP: LocalDeltaP[P]

  final def matchPartition(p: Partition): Option[LocalDeltaP[p.type]] =
    Partition.testEq[P, p.type](this.p, p).map(_ subst deltaP)
}

// Space:
//   UpdateSet  ∈ O(n)
//   UpdateSetG ∈ O(1)
//   UpdateSets ∈ O(2m + n)
object LocalDelta {

  @tailrec
  def filter(p: Partition, s: LocalDelta): LocalDeltaP[p.type] = s match {
    case h :: t =>
      val hp = h.p
      Partition.testEq[h.P, p.type](hp, p) match {
        case Some(ev) => ev.subst(h.deltaP)
        case None     => filter(p, t)
      }
    case Nil => LocalDeltaP[p.type](UnivEq.emptySet(p.ui), Nil)
  }
}
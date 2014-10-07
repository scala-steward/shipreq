package shipreq.webapp.shared.data.delta

import scala.annotation.tailrec

case class LocalDeltaP[P <: Partition](
  del: List[P#Id],
  upd: List[P#Instance])

trait LocalDeltaG {
  type P <: Partition
  def p: P
  def deltaP: LocalDeltaP[P]
}

// Space:
//   UpdateSet  ∈ O(n)
//   UpdateSetG ∈ O(1)
//   UpdateSets ∈ O(2m + n)
object LocalDeltas {

  @tailrec
  def filter[P <: Partition](p: P, s: LocalDeltas): LocalDeltaP[P] = s match {
    case h :: t =>
      Partition.testEq(h.p, p) match {
        case Some(ev) => ev.subst(h.deltaP)
        case None     => filter(p, t)
      }
    case Nil => LocalDeltaP(Nil, Nil)
  }
}
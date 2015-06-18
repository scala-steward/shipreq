package shipreq.webapp.client.delta

import shipreq.base.util.{IMap, UnivEq}
import shipreq.webapp.base.delta.Partition

/**
 * Delta for a partition, after being processed by [[RemoteDeltaAp]].
 *
 * P suffix = Partition.
 */
trait LocalDeltaP {
  val partition: Partition
  val delete: Set[partition.Id]
  val update: List[partition.Data]

  override def toString = s"δᵖ($partition)($delete, $update)"

  def isEmpty = delete.isEmpty && update.isEmpty
  def nonEmpty = !isEmpty

  // TODO Better way? (Partition + asInstanceOf)
  import LocalDeltaP.Aux
  import Partition._
  def fold[A](a: Aux[CustomIssueTypes.type] => A,
              b: Aux[CustomReqTypes  .type] => A,
              c: Aux[Fields          .type] => A,
              d: Aux[Tags            .type] => A): A = {
    def force[P <: Partition]: Aux[P] = this.asInstanceOf[Aux[P]]
    partition match {
      case CustomIssueTypes => a(force)
      case CustomReqTypes   => b(force)
      case Fields           => c(force)
      case Tags             => d(force)
    }
  }
}

object LocalDeltaP {
  type Aux[P <: Partition] = LocalDeltaP {val partition: P}

  def apply(p: Partition)(del: Set[p.Id], upd: List[p.Data])(implicit ev: UnivEq[p.Id]): Aux[p.type] =
    new LocalDeltaP {
      override val partition: p.type = p
      override val delete            = del
      override val update            = upd
    }

  def empty(p: Partition)(implicit ev: UnivEq[p.Id]): Aux[p.type] =
    apply(p)(UnivEq.emptySet, Nil)
}

// =====================================================================================================================

case class LocalDelta(data: IMap[Partition, LocalDeltaP]) extends AnyVal {

  def get(p: Partition): Option[LocalDeltaP.Aux[p.type]] =
    data.get(p).asInstanceOf[Option[LocalDeltaP.Aux[p.type]]] // TODO asInstanceOf

  def apply(p: Partition)(implicit ev: UnivEq[p.Id]): LocalDeltaP.Aux[p.type] =
    get(p).getOrElse(LocalDeltaP empty p)

  def +(d: LocalDeltaP): LocalDelta = {
    assert(!data.containsV(d))
    LocalDelta(data + d)
  }
}

object LocalDelta {
  val empty = LocalDelta(IMap.empty(_.partition))
}

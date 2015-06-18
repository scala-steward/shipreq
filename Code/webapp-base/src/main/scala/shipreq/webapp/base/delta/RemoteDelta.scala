package shipreq.webapp.base.delta

import shipreq.base.util.UnivEq
import shipreq.webapp.base.data.{RevRange, Rev}

/**
 * Delta for a partition, sent by server to client.
 *
 * P suffix = Partition.
 */
trait RemoteDeltaP {
  val partition: Partition
  val delete: Set[partition.Id]
  val update: List[partition.Data]

  override def toString = s"Δᵖ($partition)($delete, $update)"

  def isEmpty = delete.isEmpty && update.isEmpty
  def nonEmpty = !isEmpty

  // TODO Better way? (Partition + asInstanceOf)
  import RemoteDeltaP.Aux
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

object RemoteDeltaP {
  type Aux[P <: Partition] = RemoteDeltaP {val partition: P}

  def apply(p: Partition)(del: Set[p.Id], upd: List[p.Data])(implicit ev: UnivEq[p.Id]): Aux[p.type] =
    new RemoteDeltaP {
      override val partition: p.type = p
      override val delete            = del
      override val update            = upd
    }
}

// =====================================================================================================================

object RemoteDeltaPR {
  def apply(p: Partition, revRange: RevRange)(del: Set[p.Id], upd: List[p.Data])(implicit ev: UnivEq[p.Id]): RemoteDeltaPR =
    new RemoteDeltaPR(RemoteDeltaP(p)(del, upd), revRange)

  def apply(d: RemoteDeltaP, revRange: RevRange): RemoteDeltaPR =
    new RemoteDeltaPR(d, revRange)
}

/**
 * Delta for a partition, and partition-specific revisions for which the delta is applicable.
 * Sent by server to client.
 *
 * PR suffix = Partition & Revision.
 */
final class RemoteDeltaPR private(val delta: RemoteDeltaP, val revRange: RevRange) {

  def partition: Partition =
    delta.partition

  override def toString = s"Δᵖʳ($revRange $delta)"

  def isEmpty = delta.isEmpty
  def nonEmpty = !isEmpty

  def applicability(target: Rev): Applicability =
    if (revRange contains target)
      Applicable
    else if (target > revRange.toInclusive)
      Irrelevant
    else
      Inapplicable
}

// =====================================================================================================================

sealed trait Applicability
case object Applicable   extends Applicability
case object Inapplicable extends Applicability
case object Irrelevant   extends Applicability

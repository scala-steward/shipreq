package shipreq.webapp.base.data

import japgolly.nyaya._
import shipreq.base.util.TaggedTypes.TaggedLong

/** A monotonic revision number. */
case class Rev(value: Long) extends TaggedLong {
  @inline def succ      = Rev(value + 1L)
  @inline def +(r: Rev) = Rev(value + r.value)
}

// =====================================================================================================================

case class RevRange(fromInclusive: Rev, toInclusive: Rev) {
  this assertSatisfies RevRange.prop

  override def toString = s"[${fromInclusive.value}-${toInclusive.value}]"

  def contains(rev: Rev): Boolean =
    (rev >= fromInclusive) && (rev <= toInclusive)
}

object RevRange {
  def single(rev: Rev) = RevRange(rev, rev)

  lazy val prop =
    Prop.test[RevRange]("from ≥ 0", _.fromInclusive >= 0) ∧
    Prop.test[RevRange]("from ≤ to", r => r.fromInclusive <= r.toInclusive)
}
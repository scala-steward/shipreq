package shipreq.webapp.base.data

import scala.collection.TraversableLike
import shipreq.base.util.IsoBool
import shipreq.base.util.univeq._

sealed abstract class FilterDead(val filter: Option[Live => Boolean]) extends IsoBool[FilterDead] {
  override final def companion = FilterDead

  // TODO Rename FilterDead methods

  final def apply[A, C[x] <: TraversableLike[x, C[x]]](as: C[A])(f: => (A => Live)): C[A] =
    filter.fold(as)(g => as.filter(g compose f))

  final def apply[A](as: Iterator[A])(f: => (A => Live)): Iterator[A] =
    filter.fold(as)(g => as.filter(g compose f))

  final val filterFn: Live => Boolean =
    filter.getOrElse(_ => true)

  final def filterFnA[A](f: A => Live): A => Boolean =
    filter.fold((_: A) => true)(_ compose f)

  def ldStatAccessor[A]: LDStat[A] => A

  final def ldStatsAccessor[K, A](stats: LDStats[K, A]): K => A = {
    val get = ldStatAccessor[A]
    k => get(stats(k))
  }
}

object FilterDead extends IsoBool.Object[FilterDead] {
  override def positive = HideDead
  override def negative = ShowDead
}

case object HideDead extends FilterDead(Some(_ ==* Live)) {
  override def ldStatAccessor[A] = _.live
}

case object ShowDead extends FilterDead(None) {
  override def ldStatAccessor[A] = _.all
}

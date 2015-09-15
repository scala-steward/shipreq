package shipreq.webapp.client.lib

import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra.Reusability
import scala.collection.GenTraversableLike
import shipreq.base.util.IsoBool
import shipreq.webapp.base.data.{LDStats, LDStat, Live}

sealed trait FilterDead {
  val filter: Option[Live => Boolean]

  final def apply[A, C[x] <: GenTraversableLike[x, C[x]]](as: C[A])(f: => (A => Live)): C[A] =
    filter.fold(as)(g => as.filter(g compose f))

  final def filterFn: Live => Boolean =
    filter.getOrElse(_ => true)

  final def filterFnA[A](f: A => Live): A => Boolean =
    filter.fold((_: A) => true)(_ compose f)

  def ldStatAccessor[A]: LDStat[A] => A

  final def ldStatsAccessor[K, A](stats: LDStats[K, A]): K => A = {
    val get = ldStatAccessor[A]
    k => get(stats(k))
  }
}

object FilterDead extends IsoBool.ObjOnly[FilterDead] {
  override protected def pos = HideDead
  override protected def neg = ShowDead
  implicit val reusability = Reusability.byEqual[FilterDead]
}

case object HideDead extends FilterDead with IsoBool[FilterDead] {
  override protected def neg = ShowDead
  override val filter: Option[Live => Boolean] = Some(Live.equality.equal(Live, _))
  override def ldStatAccessor[A] = _.live
}

case object ShowDead extends FilterDead with IsoBool[FilterDead] {
  override protected def neg = HideDead
  override val filter: Option[Live => Boolean] = None
  override def ldStatAccessor[A] = _.all
}

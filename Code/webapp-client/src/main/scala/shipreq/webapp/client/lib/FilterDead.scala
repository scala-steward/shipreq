package shipreq.webapp.client.lib

import scala.collection.{GenTraversableLike, GenTraversable}
import scalaz.Isomorphism.<=>
import shipreq.base.util.UnivEq
import shipreq.webapp.base.data.Alive

sealed trait FilterDead {
  val filter: Option[Alive => Boolean]

  def apply[A, C[x] <: GenTraversableLike[x, C[x]]](as: C[A])(f: => (A => Alive)): C[A] =
    filter.fold(as)(g => as.filter(g compose f))

  def filterFn: Alive => Boolean =
    filter.getOrElse(_ => true)

  def filterFnA[A](f: A => Alive): A => Boolean =
    filter.fold((_: A) => true)(_ compose f)
}

object FilterDead {
  @inline implicit def equality = UnivEq.force[FilterDead]
}

case object ShowDead extends FilterDead with (Boolean <=> FilterDead) {
  override val from  : FilterDead => Boolean    = _ == ShowDead
  override val to    : Boolean => FilterDead    = if (_) ShowDead else HideDead
  override val filter: Option[Alive => Boolean] = None
}

case object HideDead extends FilterDead with (Boolean <=> FilterDead) {
  override val from  : FilterDead => Boolean    = _ == HideDead
  override val to    : Boolean => FilterDead    = if (_) HideDead else ShowDead
  override val filter: Option[Alive => Boolean] = Some(Alive.equality.equal(Alive, _))
}

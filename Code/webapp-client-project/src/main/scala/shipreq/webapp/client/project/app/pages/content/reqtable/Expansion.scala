package shipreq.webapp.client.project.app.pages.content.reqtable

import japgolly.univeq.UnivEq
import monocle.macros.Lenses
import scalaz.{Equal, Semigroup}
import shipreq.base.util._

@Lenses
final case class Expansion[A](values: Vector[A], original: Set[A]) {
  def result: Vector[A] =
    values
}

object Expansion {

  private val emptyInstance: Expansion[AnyRef] =
    apply(Vector.empty, Set.empty)

  def empty[A]: Expansion[A] =
    emptyInstance.asInstanceOf[Expansion[A]]

  def one[A: UnivEq](a: A): Expansion[A] =
    apply(Vector1(a), Set.empty[A] + a)

  implicit def univEq[A: UnivEq]: UnivEq[Expansion[A]] = UnivEq.derive

  implicit def semigroup[A]: Semigroup[Expansion[A]] = {
    implicit val e: Equal[A] = Equal.equalA // Because we have a Set in expansion
    @inline implicit def u: UnivEq[A] = UnivEq.force // Because we have a Set in expansion
    new Semigroup[Expansion[A]] {
      override def append(x: Expansion[A], yy: => Expansion[A]) = {
        val y = yy
        Expansion(
          values   = Util.vectorConcatDistinct(x.values, y.values)(e),
          original = Util.mergeSets(x.original, y.original),
        )
      }

    }
  }
}

package shipreq.base.util.fp

import cats.Semigroup

trait Monoid[@specialized(Int) A] extends Semigroup[A] {
  def empty: A
}

object Monoid {

  object IntAddition extends Monoid[Int] {
    override final val empty = 0
    override def combine(a: Int, b: Int) = a + b
  }

  private def set[A]: Monoid[Set[A]] =
    new Monoid[Set[A]] {
      override def empty = Set.empty[A]
      override def combine(a: Set[A], b: Set[A]) = a ++ b
    }

  private def vector[A]: Monoid[Vector[A]] =
    new Monoid[Vector[A]] {
      override def empty = Vector.empty[A]
      override def combine(a: Vector[A], b: Vector[A]) = a ++ b
    }

  object Implicits {

    @inline implicit def monoidIntAddition: Monoid[Int] =
      IntAddition

    private[this] val _vector = vector[Any]

    implicit def monoidVector[A]: Monoid[Vector[A]] =
      _vector.asInstanceOf[Monoid[Vector[A]]]

    private[this] val _set = set[Any]

    implicit def monoidSet[A]: Monoid[Set[A]] =
      _set.asInstanceOf[Monoid[Set[A]]]
  }
}
package shipreq.webapp.base.data

import scalaz.OneAnd
import scalaz.Isomorphism.<=>
import shipreq.base.util.TaggedTypes._
import shipreq.base.util.UnivEq


final case class Rev(value: Long) extends TaggedLong {
  @inline def succ      = Rev(value + 1L)
  @inline def +(r: Rev) = Rev(value + r.value)
}


sealed abstract class Alive {
//  def fold[A](alive: => A, dead: => A): A
}
case object Alive extends Alive with (Boolean <=> Alive) {
  @inline implicit def equality = UnivEq.force[Alive]
  override val from             = equality.equal(Alive, _: Alive)
  override val to               = if (_: Boolean) Alive else Dead

//  override def fold[A](alive: => A, dead: => A): A = alive
}
case object Dead extends Alive {
//  override def fold[A](alive: => A, dead: => A): A = dead
}


sealed trait ImplicationRequired
case object ImplicationRequired extends ImplicationRequired with (Boolean <=> ImplicationRequired) {
  @inline implicit def equality = UnivEq.force[ImplicationRequired]
  override val from             = equality.equal(ImplicationRequired, _: ImplicationRequired)
  override val to               = if (_: Boolean) ImplicationRequired else Not
  case object Not extends ImplicationRequired
}


/**
 * A key by which users can refer to data.
 * These references require a hashtag prefix.
 *
 * Examples:
 * #TBD refers to a custom issue type.
 * #pri=high refers to a grouping.
 */
final case class HashRefKey(value: String) extends TaggedString


/**
 * An intensional subset over F[A].
 */
sealed abstract class ISubset[F[_], A] {
//  final def filter: Option[A => Boolean] = {
//    @inline def check(a: A, as: OneAnd[Set, A]) = a == as.head || as.tail.contains(a)
//    this match {
//      case Subset.All()    => None
//      case Subset.Only(as) => Some(check(_, as))
//      case Subset.Not(as)  => Some(!check(_, as))
//    }
//  }
}
object ISubset {
  final case class All [F[_], A]()                     extends ISubset[F, A]
  final case class Only[F[_], A](values: OneAnd[F, A]) extends ISubset[F, A]
  final case class Not [F[_], A](values: OneAnd[F, A]) extends ISubset[F, A]

  @inline implicit def univEquality[F[_], A](implicit v: UnivEq[OneAnd[F, A]]): UnivEq[ISubset[F, A]] =
    UnivEq.force
}
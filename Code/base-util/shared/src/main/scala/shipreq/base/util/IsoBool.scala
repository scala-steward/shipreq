package shipreq.base.util

import japgolly.univeq.UnivEq
import scalaz.Isomorphism.<=>
import IsoBool._

/**
 * Boolean isomorphism.
 *
 * Mix into the base type and override [[this.companion]] there.
 */
trait IsoBool[B <: IsoBool[B]] extends (Boolean <=> B) with Product with Serializable {
  this: B =>

  def companion: Object[B]

  final def unary_! : B =
    if (this == companion.positive)
      companion.negative
    else
      companion.positive

  @inline final def is(b: B): Boolean =
    b == this

  @inline final def when(cond: Boolean): B =
    if (cond) this else !this

  final override val from = is(_)
  final override val to   = when(_)

  final def fnToThisWhen[A](f: A => Boolean): A => B =
    to compose f

  final def fnToThisWhen[A](b: Boolean <=> A): A => B =
    fnToThisWhen(b.from)

  final def <=>[A <: IsoBool[A]](A: IsoBool[A]): B <=> A =
    new (B <=> A) {
      override val from: A => B = IsoBool.this fnToThisWhen A
      override val to  : B => A = A fnToThisWhen IsoBool.this
    }
}

object IsoBool {

  /**
   * Mix into the companion object for the type.
   */
  trait Object[B <: IsoBool[B]] {
    implicit final def equality: UnivEq[B] = UnivEq.force

    def positive: B with IsoBool[B]
    def negative: B with IsoBool[B]

    final def memo[A](f: B => A): B => A = {
      val p = f(positive)
      val n = f(negative)
      b => if (b is positive) p else n
    }

    final def memoLazy[A](f: B => A): B => A = {
      lazy val p = f(positive)
      lazy val n = f(negative)
      b => if (b is positive) p else n
    }

    final def fold[A](a: A)(f: (A, B) => A): A =
      f(f(a, positive), negative)

    final def mapReduce[X, Y](m: B => X)(r: (X, X) => Y): Y =
      r(m(positive), m(negative))

    final def forall(f: B => Boolean): Boolean =
      f(positive) && f(negative)

    final def exists(f: B => Boolean): Boolean =
      f(positive) || f(negative)
  }

  /**
   * Adds boolean ops with `companion.positive` being the equivalent of `true`.
   */
  trait WithBoolOps[B <: IsoBool[B]] extends IsoBool[B] {
    this: B =>

    final def &(that: => B): B = {
      val pos = companion.positive
      pos when ((this is pos) && (that is pos))
    }

    final def &&(that: => Boolean): B = {
      val pos = companion.positive
      pos when ((this is pos) && that)
    }

    final def |(that: => B): B = {
      val pos = companion.positive
      pos when ((this is pos) || (that is pos))
    }

    final def ||(that: => Boolean): B = {
      val pos = companion.positive
      pos when ((this is pos) || that)
    }
  }
}

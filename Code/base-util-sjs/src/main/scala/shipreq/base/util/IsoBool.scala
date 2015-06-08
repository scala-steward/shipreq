package shipreq.base.util

import scalaz.Isomorphism.<=>

/**
 * Boolean isomorphism.
 */
trait IsoBool[B] extends (Boolean <=> B) {
  this: B =>

  protected def neg: B

  final val :: : B => Boolean = _ == this
  final val <~ : Boolean => B = if (_) this else neg

  final override def from = ::
  final override def to   = <~

  final def when[A](i: IsoBool[A]): A => B =
    a => this <~ (a :: i)

  final def <=>[A](i: IsoBool[A]): B <=> A =
    new (B <=> A) {
      override val from: A => B = IsoBool.this when i
      override val to  : B => A = i when IsoBool.this
    }

  final def negate(b: B): B =
    if (b :: this) neg else this

  final def negate[N](n: N, b: B)(implicit N: Numeric[N]): N =
    if (b :: this) N.negate(n) else n

  final def signum(b: B): Int =
    if (b :: this) 1 else -1
}

object IsoBool {

  trait ObjOnly[B] {
    implicit final def equality = UnivEq.force[B]
    protected def pos: B with IsoBool[B]
    protected def neg: B

    final def memo[A](f: B => A): B => A = {
      val p = f(pos)
      val n = f(neg)
      b => if (b :: pos) p else n
    }
  }

  /**
   * Boolean isomorphism: case + object.
   */
  trait Obj[B] extends IsoBool[B] with ObjOnly[B] {
    this: B =>
    final override protected def pos = this
  }
}
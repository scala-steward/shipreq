package shipreq.base.util

import scalaz.{Order, Equal}
import scalaz.std.anyVal.intInstance

/**
 * Universal equality.
 */
trait UnivEq[A] extends Equal[A] {
  final override def equalIsNatural = true
  final override def equal(a: A, b: A) = a == b
  final def sharedInstance = UnivEq.on[A]
}

object UnivEq {
  @inline def apply[F](implicit u: UnivEq[F]): UnivEq[F] = u

  private[this] val instance = new UnivEq[Any] {}

  @inline def on[A]: UnivEq[A] = instance.asInstanceOf[UnivEq[A]]

  @inline implicit def string  = on[String]
  @inline implicit def long    = on[Long]
  @inline implicit def int     = on[Int]
  @inline implicit def short   = on[Short]
  @inline implicit def boolean = on[Boolean]

  @inline implicit def set [A: UnivEq] = on[Set[A]]
  @inline implicit def list[A: UnivEq] = on[List[A]]

  def withArbitraryOrder[A](values: List[A]): Order[A] with UnivEq[A] = {
    val fixedOrder = values.zipWithIndex.toMap
    new Order[A] with UnivEq[A] {
      @inline private[this] def int(s: A) = fixedOrder(s)
      override def order(a: A, b: A) = Order[Int].order(int(a), int(b))
    }
  }
}
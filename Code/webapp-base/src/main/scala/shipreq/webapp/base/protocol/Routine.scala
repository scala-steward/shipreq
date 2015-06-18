package shipreq.webapp.base.protocol

import scalaz.Equal
import upickle.{Reader, Writer}

object Routine {

  /**
   * Description of a server-side routine (function).
   */
  abstract class Desc {
    type I
    type O
    implicit def ri: Reader[I]
    implicit def wi: Writer[I]
    implicit def ro: Reader[O]
    implicit def wo: Writer[O]
    final type Remote = Routine.Remote[this.type]
  }

  /** Syntactic convenience that allows for single-line declaration. */
  abstract class =>|=>[_I, _O](implicit RI: Reader[_I], WI: Writer[_I], RO: Reader[_O], WO: Writer[_O]) extends Desc {
    final override type I = _I
    final override type O = _O
    final override implicit def ri = RI
    final override implicit def wi = WI
    final override implicit def ro = RO
    final override implicit def wo = WO
  }

  type Aux[_I, _O] = Desc {type I = _I; type O = _O}

  /**
   * A routine ready for remote invocation.
   *
   * @param n The server-side Lift function key.
   */
  final case class Remote[D <: Desc](n: String, d: D)

  implicit def equalDesc[D <: Desc]: Equal[D] = Equal.equalRef
  implicit def equalRemove[D <: Desc]: Equal[Remote[D]] =
    Equal.equal((a, b) => a.n == b.n && (a.d eq b.d))
}
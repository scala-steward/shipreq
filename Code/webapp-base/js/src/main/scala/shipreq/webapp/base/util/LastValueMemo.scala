package shipreq.webapp.base.util

import japgolly.scalajs.react.Reusability
import scala.runtime.AbstractFunction1
import shipreq.base.util.FreeOption

/** Pretty much the same intent as `Px` except that unlike `Px`, it doesn't have access to some "current" value.
  * The input value is always provided on execution. Therefore this behaves like a normal function.
  */
final class LastValueMemo[A, B](initialResult: FreeOption[LastValueMemo.Result[A, B]],
                                f: A => B,
                                val reusability: Reusability[A]) extends AbstractFunction1[A, B] {
  import LastValueMemo.Result

  private var lastResult: FreeOption[Result[A, B]] =
    initialResult

  override def apply(a: A): B =
    if (lastResult.nonEmpty && reusability.test(lastResult.getOrNull.a, a))
      lastResult.getOrNull.b
    else {
      val b = f(a)
      lastResult = FreeOption(new Result(a, b))
      b
    }

  def map[C](g: B => C): LastValueMemo[A, C] =
    new LastValueMemo(lastResult.map(_.map(g)), g compose f, reusability)
}

object LastValueMemo extends LastValueMemoBoilerplate {

  def apply[A, B](f: A => B)(implicit r: Reusability[A]): LastValueMemo[A, B] =
    new LastValueMemo(FreeOption.empty, f, r)

  private final class Result[A, B](val a: A, val b: B) {
    def map[C](f: B => C): Result[A, C] =
      new Result(a, f(b))
  }
}
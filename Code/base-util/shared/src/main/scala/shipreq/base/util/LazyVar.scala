package shipreq.base.util

/**
  * Lazy variable.
  *
  * NOT thread-safe.
  */
final class LazyVar[A](thunk: () => A) {

  private[this] var thunkRef = thunk
  private[this] var value: A = _

  def initialised: Boolean =
    thunkRef eq null

  def get(): A = {
    if (!initialised)
      set(thunkRef())
    value
  }

  def set(a: A): Unit = {
    thunkRef = null
    value = a
  }

  def mod(f: A => A): Unit =
    set(f(get()))
}

object LazyVar {
  def apply[A](a: => A): LazyVar[A] =
    new LazyVar(() => a)
}
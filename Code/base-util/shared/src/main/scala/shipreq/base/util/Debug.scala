package shipreq.base.util

import scala.annotation.elidable

/**
 * Methods to help with debugging.
 */
object Debug {

  implicit class DebugAnyExt[T](val v: T) extends AnyVal {
    def pp(): T = {println(v); v}
    def pp(title: Any): T = {println(s"$title: $v"); v}
    def pp(f: T => Any): T = {println(f(v)); v}
  }

  def time[A](name: String)(a: => A): A = {
    val start  = __start
    val result = a
    __stop(name, start)
    result
  }

  import System.{nanoTime => clock}

  private type Clock = Long

  @elidable(elidable.FINE)
  private def __start: Clock =
    clock()

  @elidable(elidable.FINE)
  private def __stop(name: String, start: Clock): Unit = {
    val end = clock()
    val timeInMs = (end - start).toDouble / 1000000
    printf("[%s] Completed in %.3f ms.\n", name, timeInMs)
  }
}

trait DebugImplicits {
  import Debug._
  implicit def debugAnyExt[T](v: T) = DebugAnyExt(v)
}
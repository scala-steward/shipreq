package shipreq.webapp.base.util

import japgolly.scalajs.react.Reusability
import scala.runtime.AbstractFunction1

final class LruMemo[A, B](f: A => B, maxSize: Int, isSame: (A, A) => Boolean) extends AbstractFunction1[A, B] {
  import LruMemo.Result

  assert(maxSize > 0)

  private val cache = new Array[Result[A, B]](maxSize)
  private var time = 0
  private var size = 0

  override def apply(a: A): B = {
    val t = time
    time += 1

    // Check if acceptable result exists
    var i = 0
    var minTime = -1
    var minIdx = -1
    while (i < size) {
      val r = cache(i)
      if (isSame(r.a, a)) {
        r.lastAccessed = t
        return r.b
      }

      if (minIdx == -1 || r.lastAccessed < minTime) {
        minIdx = i
        minTime = r.lastAccessed
      }

      i += 1
    }

    // Not found. Calculate.
    val b = f(a)

    // Add to cache
    val r = new Result(a, b, t)
    if (size < maxSize) {
      cache(size) = r
      size += 1
    } else {
      cache(minIdx) = r
    }

    b
  }
}

object LruMemo {

  def byUnivEq[A, B](f: A => B, maxSize: Int)(implicit e: UnivEq[A]): LruMemo[A, B] =
    new LruMemo(f, maxSize, e.univEq)


  def byReusability[A, B](f: A => B, maxSize: Int)(implicit r: Reusability[A]): LruMemo[A, B] =
    new LruMemo(f, maxSize, r.test)

  private final class Result[A, B](val a: A,
                                   val b: B,
                                   var lastAccessed: Int)
}
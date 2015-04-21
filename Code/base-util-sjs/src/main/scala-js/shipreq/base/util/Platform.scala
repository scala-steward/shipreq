package shipreq.base.util

import scala.collection.mutable.AnyRefMap

// ================
// ====        ====
// ====   JS   ====
// ====        ====
// ================

object Platform {

  def memo[A <: AnyRef : UnivEq, B](f: A => B): A => B = {
    val cache = new AnyRefMap[A, B](128)
    a => cache.getOrElseUpdate(a, f(a))
  }

}

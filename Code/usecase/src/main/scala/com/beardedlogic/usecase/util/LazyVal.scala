package com.beardedlogic.usecase.util

/**
 * A `lazy val` that can be passed around.
 *
 * @param init The function that returns the value.
 * @tparam T The value type.
 */
case class LazyVal[T](init: () => T) {
  lazy val get: T = init()
  @inline final def apply(): T = get
}

object LazyVal {

  /**
   * Convenience method for creating a LazyVal. Removes the need to write `() =>`
   *
   * @param t A lazily-evaluated function.
   */
  def <~[T](t: => T) = apply(() => t)
}

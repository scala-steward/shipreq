package com.beardedlogic.usecase.lib

trait InitialValue[T] {
  def apply(): T
}

object InitialValue {
  def apply[T](t: T) = new InitialValue[T] {def apply = t}
}

object InitialValues {
  implicit def InitialValueBiMap[A, B] = InitialValue[BiMap[A, B]](BiMap.empty)
  implicit def InitialValueMap[A, B] = InitialValue[Map[A, B]](Map.empty)
  implicit def InitialValueList[T] = InitialValue[List[T]](List.empty)
  def InitiallyNull[T >: Null] = InitialValue[T](null)
}

trait CachedFunctionLike[R] {
  protected var cache: R

  @inline final def get: R = cache

  /**
   * Creates a copy one step removed from the original function. Instead the copy will depend on this class's cached
   * value.
   */
  def dependentCopy = new CachedFunctionDependent[R](this)

  /** Manually sets the underlying cache value. */
  def <<(newValue: R) { cache = newValue }
}

/**
 * @since 4/06/2013
 */
object CachedFunction {
  def apply[R](fn: => R)(implicit initialValue: InitialValue[R]) = new CachedFunction[R](initialValue(), fn)
  def eager[R](fn: => R) = new CachedFunction[R](fn, fn)
}

object CachedFunction1 {
  def apply[T, R](fn: T => R)(implicit initialValue: InitialValue[R]) = new CachedFunction1[T, R](initialValue(), fn)
  def eager[T, R](fn: T => R)(arg: T) = new CachedFunction1[T, R](fn(arg), fn)
  def static[T, R](staticValue: R) = new CachedFunction1[T, R](staticValue, (_: T) => staticValue)
}

/**
 * Caches the result of a zero-arg function, and allows manual invalidation.
 *
 * @since 4/06/2013
 */
class CachedFunction[R](initialValue: R, fn: => R) extends CachedFunctionLike[R] {

  override protected var cache: R = initialValue

  @inline final def refresh: R = { cache = fn; cache }

  override def toString = s"CachedFunction($get)"

  @inline final def ifStale(block: => Any): Unit = {
    val newValue = fn
    if (newValue != cache) {
      cache = newValue
      block
    }
  }

  /** Creates an independent copy of this class. */
  def copy = new CachedFunction[R](get, fn)
}

class CachedFunctionDependent[R](val dependingOn: CachedFunctionLike[R])
  extends CachedFunction[R](dependingOn.get, dependingOn.get)

class CachedFunction1[-T, R](initialValue: R, fn: T => R) extends CachedFunctionLike[R] {

  override protected var cache: R = initialValue

  @inline final def refresh(arg: T): R = { cache = fn(arg); cache }

  override def toString = s"CachedFunction1($get)"

  @inline final def ifStale(arg: T)(block: => Any): Unit = {
    val newValue = fn(arg)
    if (newValue != cache) {
      cache = newValue
      block
    }
  }

  /** Creates an independent copy of this class. */
  def copy = new CachedFunction1[T, R](get, fn)
}
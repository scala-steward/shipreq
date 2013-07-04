package com.beardedlogic.usecase.util

object CachedFunction {
  def lazy0[R](fn: => R) = new LazyCachedFunction[R](fn)
  def eager0[R](fn: => R) = new EagerCachedFunction[R](fn, fn)
  def eager0WithInitial[R](fn: => R)(initial: R) = new EagerCachedFunction[R](initial, fn)

  def eager1[T, R](fn: T => R)(arg: T) = new CachedFunction1[T, R](fn(arg), fn)
  def eager1WithInitial[T, R](fn: T => R)(initial: R) = new CachedFunction1[T, R](initial, fn)
  def static1[T, R](staticValue: R) = new CachedFunction1[T, R](staticValue, (_: T) => staticValue)
}

// TODO Could squeeze much more reuse out of CachedFunction classes

// =====================================================================================================================

trait CachedFunctionLike[R] {

  def get: R

  /**
   * Creates a copy one step removed from the original function. Instead the copy will depend on this class's cached
   * value.
   */
  def dependentCopyEager = new EagerCachedFunctionDependent[R](this)

  /**
   * Creates a copy one step removed from the original function. Instead the copy will depend on this class's cached
   * value.
   */
  def dependentCopyLazy = new LazyCachedFunctionDependent[R](this)
}

// =====================================================================================================================

/**
 * Caches the result of a zero-arg function, and allows manual invalidation.
 *
 * @since 4/06/2013
 */
class LazyCachedFunction[R](fn: => R) extends CachedFunctionLike[R] {
  private var cache: Option[R] = None

  @inline final def get: R = {
    if (cache.isEmpty) cache = Some(fn)
    cache.get
  }

  @inline final def invalidate() { cache = None }

  override def toString = s"LazyCachedFunction($get)"

  @inline final def ifStale(block: => Any): Unit = {
    val newValue = Some(fn)
    if (newValue != cache) {
      cache = newValue
      block
    }
  }

  /** Creates an independent copy of this class. */
  def copy = {
    val c = new LazyCachedFunction[R](fn)
    c.cache = cache
    c
  }

  /** Manually sets the underlying cache value. */
  def <<(newValue: R) { cache = Some(newValue) }
}

// =====================================================================================================================

/**
 * Caches the result of a zero-arg function, and allows manual invalidation.
 *
 * @since 4/06/2013
 */
class EagerCachedFunction[R](initialValue: R, fn: => R) extends CachedFunctionLike[R] {

  private var cache: R = initialValue

  @inline final def get: R = cache

  @inline final def refresh: R = { cache = fn; cache }

  override def toString = s"EagerCachedFunction($get)"

  @inline final def ifStale(block: => Any): Unit = {
    val newValue = fn
    if (newValue != cache) {
      cache = newValue
      block
    }
  }

  /** Creates an independent copy of this class. */
  def copy = new EagerCachedFunction[R](get, fn)

  /** Manually sets the underlying cache value. */
  def <<(newValue: R) { cache = newValue }
}

// =====================================================================================================================

class EagerCachedFunctionDependent[R](val dependingOn: CachedFunctionLike[R])
  extends EagerCachedFunction[R](dependingOn.get, dependingOn.get)

class LazyCachedFunctionDependent[R](val dependingOn: CachedFunctionLike[R])
  extends LazyCachedFunction[R](dependingOn.get)

// =====================================================================================================================

class CachedFunction1[-T, R](initialValue: R, fn: T => R) extends CachedFunctionLike[R] {

  private var cache: R = initialValue

  @inline final def get: R = cache

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

  /** Manually sets the underlying cache value. */
  def <<(newValue: R) { cache = newValue }
}
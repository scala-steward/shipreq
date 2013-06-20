package com.beardedlogic.usecase
package lib

import com.google.common.collect.MapMaker
import java.util.concurrent.locks.{Lock, ReentrantReadWriteLock}
import java.util.concurrent.{TimeoutException, TimeUnit}
import net.liftweb.common.Logger
import Locks._

object Locks {
  /** R/W locks keyed by use case data id. */
  val UseCase = new LockManager

  sealed trait ReadLockToken
  sealed trait WriteLockToken extends ReadLockToken
}

class LockManager extends Logger {

  private val ReadLockToken = new ReadLockToken{}
  private val WriteLockToken = new WriteLockToken{}

  private class Fn extends com.google.common.base.Function[Long, ReentrantReadWriteLock] {
    override def apply(key: Long) = new ReentrantReadWriteLock
    override def equals(that: Any) = false
  }

  private val lockMap = new MapMaker()
                        .concurrencyLevel(32)
                        .initialCapacity(0x1000)
                        .weakValues()
                        .makeComputingMap[Long, ReentrantReadWriteLock](new Fn)

  @inline final protected def getLock(id: Long): ReentrantReadWriteLock = lockMap.get(id)

  @inline private def withLock[U](lock: Lock)(block: => U): U = {
    if (!lock.tryLock(30, TimeUnit.SECONDS)) throw new TimeoutException()
    try block finally lock.unlock
  }

  final def withReadLock[U](id: Long)(block: => U): U = withLock(getLock(id).readLock)(block)
  final def withWriteLock[U](id: Long)(block: => U): U = withLock(getLock(id).writeLock)(block)

  final def withReadLockToken[U](id: Long)(block:ReadLockToken => U): U = withLock(getLock(id).readLock)(block(ReadLockToken))
  final def withWriteLockToken[U](id: Long)(block:WriteLockToken => U): U = withLock(getLock(id).writeLock)(block(WriteLockToken))

//  @inline final def withReadLockAndTransaction[U](id: Long, dao: DAO)(block: ReadLockToken => U): U = withReadLock(id)(dao.withTransaction(block))
//  @inline final def withWriteLockAndTransaction[U](id: Long, dao: DAO)(block: WriteLockToken => U): U = withWriteLock(id)(dao.withTransaction(block))

  def forRead[M[_]](id: Long) = new ResourceLeaseMonad1[ReadLockToken, M] {protected override def exec[T](f: ReadLockToken => T): T = withReadLockToken(id)(f(_))}
  def forReadLeft[R, M[_, R]](id: Long) = new ResourceLeaseMonadL[ReadLockToken, R, M] {protected override def exec[T](f: ReadLockToken => T): T = withReadLockToken(id)(f(_))}
  def forReadRight[L, M[L, _]](id: Long) = new ResourceLeaseMonadR[ReadLockToken, L, M] {protected override def exec[T](f: ReadLockToken => T): T = withReadLockToken(id)(f(_))}

  def forWrite[M[_]](id: Long) = new ResourceLeaseMonad1[WriteLockToken, M] {protected override def exec[T](f: WriteLockToken => T): T = withWriteLockToken(id)(f(_))}
  def forWriteLeft[R, M[_, R]](id: Long) = new ResourceLeaseMonadL[WriteLockToken, R, M] {protected override def exec[T](f: WriteLockToken => T): T = withWriteLockToken(id)(f(_))}
  def forWriteRight[L, M[L, _]](id: Long) = new ResourceLeaseMonadR[WriteLockToken, L, M] {protected override def exec[T](f: WriteLockToken => T): T = withWriteLockToken(id)(f(_))}
}

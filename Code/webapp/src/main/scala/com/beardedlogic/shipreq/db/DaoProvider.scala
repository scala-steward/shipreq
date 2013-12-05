package com.beardedlogic.shipreq
package db

import util.{ResourceLeaseMonadL, ResourceLeaseMonadR, ResourceLeaseMonad1}

trait DaoProvider {

  // TODO Delete ResourceLeaseMonad[LR]

  def withSession[T](block: DaoS => T): T
  def forSession[M[_]] = new ResourceLeaseMonad1[DaoS, M] {protected override def exec[T](f: DaoS => T): T = withSession(f(_))}
  def forSessionLeft[R, M[_, R]] = new ResourceLeaseMonadL[DaoS, R, M] {protected override def exec[T](f: DaoS => T): T = withSession(f(_))}
  def forSessionRight[L, M[L, _]] = new ResourceLeaseMonadR[DaoS, L, M] {protected override def exec[T](f: DaoS => T): T = withSession(f(_))}

  def withTransaction[T](block: DaoT => T): T
  def forTransaction[M[_]] = new ResourceLeaseMonad1[DaoT, M] {protected override def exec[T](f: DaoT => T): T = withTransaction(f(_))}
  def forTransactionLeft[R, M[_, R]] = new ResourceLeaseMonadL[DaoT, R, M] {protected override def exec[T](f: DaoT => T): T = withTransaction(f(_))}
  def forTransactionRight[L, M[L, _]] = new ResourceLeaseMonadR[DaoT, L, M] {protected override def exec[T](f: DaoT => T): T = withTransaction(f(_))}

  def withAdminDao[T](block: AdminDao => T): T
  def forAdmin[M[_]] = new ResourceLeaseMonad1[AdminDao, M] {protected override def exec[T](f: AdminDao => T): T = withAdminDao(f(_))}
}

package com.beardedlogic.shipreq
package db

import util.ResourceLeaseMonad1

trait DaoProvider {

  def withSession[T](block: DaoS => T): T
  def forSession[M[_]] = new ResourceLeaseMonad1[DaoS, M] {protected override def exec[T](f: DaoS => T): T = withSession(f(_))}

  def withTransaction[T](block: DaoT => T): T
  def forTransaction[M[_]] = new ResourceLeaseMonad1[DaoT, M] {protected override def exec[T](f: DaoT => T): T = withTransaction(f(_))}

  def withAdminDao[T](block: AdminDao => T): T
  def forAdmin[M[_]] = new ResourceLeaseMonad1[AdminDao, M] {protected override def exec[T](f: AdminDao => T): T = withAdminDao(f(_))}
}

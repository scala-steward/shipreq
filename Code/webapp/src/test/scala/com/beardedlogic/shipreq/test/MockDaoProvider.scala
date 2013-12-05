package com.beardedlogic.shipreq
package test

import org.scalatest.mock.MockitoSugar
import db.{AdminDao, DaoS, DaoT, DaoProvider}
import app.DI

/**
 * [[com.beardedlogic.shipreq.db.DaoProvider]] that creates and uses a mock DAO.
 *
 * Usage:
 *
 * {{{
 *   MockDaoProvider(dao => when(dao.xxxx).thenReturn(xxx)).install {
 *     new MySnippet().render
 *   }
 * }}}
 */
class MockDaoProvider extends DaoProvider with MockitoSugar {
  val dao = mock[DaoT]
  val adminDao = mock[AdminDao]
  override def withSession[T](block: DaoS => T): T = block(dao)
  override def withTransaction[T](block: DaoT => T): T = block(dao)
  override def withAdminDao[T](block: AdminDao => T): T = block(adminDao)

  def install[R](fn: => R): R = DI.DaoProvider.doWith(this)(fn)
}

object MockDaoProvider {
  def apply(setup: (DaoT => Unit) = identity) = {
    val p = new MockDaoProvider
    setup(p.dao)
    p
  }
}

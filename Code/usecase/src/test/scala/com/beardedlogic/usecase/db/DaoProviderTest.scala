package com.beardedlogic.usecase.db

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.Matchers
import com.beardedlogic.usecase.test.TestDB

class DaoProviderTest extends FunSuite with Matchers with BeforeAndAfterAll {

  def dp = DB.DaoProvider

  override def beforeAll() {
    TestDB.init()
  }

  test("New session should not be in a transaction") {
    dp.withSession(_.session.conn.getAutoCommit shouldBe true)
  }
  test("New transaction should be in a transaction") {
    dp.withTransaction(_.session.conn.getAutoCommit shouldBe false)
  }
  test("Session -> transaction") {
    dp.withSession(d => {
      d.session.conn.getAutoCommit shouldBe true
      d.withTransaction(d.session.conn.getAutoCommit shouldBe false)
      d.session.conn.getAutoCommit shouldBe true
    })
  }
}

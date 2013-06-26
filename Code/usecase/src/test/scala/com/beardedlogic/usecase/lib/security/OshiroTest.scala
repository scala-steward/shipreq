package com.beardedlogic.usecase
package lib.security

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc._
import org.scalatest.{BeforeAndAfterAll, FunSpec}
import scala.slick.jdbc.{StaticQuery => Q}
import lib.db.DB
import test.TestDatabaseSupport

class OshiroTest extends FunSpec with TestDatabaseSupport with BeforeAndAfterAll {

  override val wrapTestsInTransaction = false

  case class TestUser(username: String, email: String, password: String) {
    val (hashedPassword, salt) = Oshiro.hashWithRandomSalt(password)
  }

  case class PendingTestUser(email: String, token: String)

  val user1 = TestUser("golly", "g@g.com", "hello1234")
  val user2 = TestUser("deepti", "d@d.com", "harvest321")
  val users = List(user1, user2)

  val pendingUser1 = PendingTestUser("a@a.com", "12345678901234567890")
  val pendingUsers = List(pendingUser1)

  override def beforeAll {
    super.beforeAll
    TestDatabaseSupport.init()

    DB.withInstance(true)(db => {

      // Insert mock users (registered)
      val i1 = Q.update[(String, String, String, String)]("INSERT INTO usr(username, email, password, password_salt, password_changed_at, confirmation_sent_at, confirmed_at) VALUES(?,?,?,?,NOW(),NOW(),NOW())")
      users.foreach(u => i1.execute(u.username, u.email, u.hashedPassword, u.salt)(db))

      // Insert mock users (pending confirmation)
      val i2 = Q.update[(String, String)]("INSERT INTO usr(email, confirmation_token, confirmation_sent_at) VALUES(?,?,NOW())")
      pendingUsers.foreach(u => i2.execute(u.email, u.token)(db))
    })
  }

  describe("Authentication") {
    def login(username: String, password: String) {
      SecurityUtils.getSubject.login(new UsernamePasswordToken(username, password))
    }

    it("should allow users by username") {
      login(user1.username, user1.password)
    }

    it("should allow users by email address") {
      login(user1.email, user1.password)
    }

    it("should deny when username/email doesnt exist") {
      intercept[UnknownAccountException](login("blah", user1.password))
    }

    it("should deny when password is incorrect") {
      intercept[IncorrectCredentialsException](login(user1.username, user2.password))
    }

    it("should deny when user hasnt completed registration") {
      intercept[UnknownAccountException](login(pendingUser1.email, ""))
    }
  }
}
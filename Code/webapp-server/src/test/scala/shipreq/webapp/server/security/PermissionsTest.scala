package shipreq.webapp.server.security

import org.scalatest.{FunSpec, Matchers}
import shipreq.taskman.api.{EmailAddr, UserId}
import shipreq.webapp.base.data._
import shipreq.webapp.server.data._

class PermissionsTest extends FunSpec with Matchers {
  implicit def autoUsername(a: String) = Username(a)
  implicit def autoEmailAddr(a: String) = EmailAddr(a)

  val admin = UserDescriptor(UserId(1), "ad", "ad@ad.com", Set(Roles.Admin.name))
  val joe = UserDescriptor(UserId(2), "joe", "joe@ad.com", Set.empty)

  describe("admin") {
    it("should allow admin") {
      Permissions.admin.using(user = Some(admin)).isPass shouldBe true
    }
    it("should deny anon") {
      Permissions.admin.using(user = None).isPass shouldBe false
    }
    it("should deny normal users") {
      Permissions.admin.using(user = Some(joe)).isPass shouldBe false
    }
  }
}

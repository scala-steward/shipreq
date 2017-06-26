package shipreq.webapp.server.app

import utest._
import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.server.app.AppSiteMap.Implicits._
import shipreq.webapp.server.app.AppSiteMap._

object AppSiteMapTest extends TestSuite {

  val homeRel = "/"
  val homeAbs = "http://localhost:8090"
  val logoutRel = "/logout"
  val logoutAbs = "http://localhost:8090/logout"

  override def tests = TestSuite {
    "Home relativeUrl" - assertEq(Home.relativeUrl, homeRel)
    "Home absoluteUrl" - assertEq(Home.absoluteUrl, homeAbs)
    "Logout relativeUrl" - assertEq(Logout.relativeUrl, logoutRel)
    "Logout absoluteUrl" - assertEq(Logout.absoluteUrl, logoutAbs)
  }
}

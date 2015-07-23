package shipreq.webapp.server.snippet

import shipreq.webapp.server.test.TestDatabaseSupport
import shipreq.webapp.server.util.NonEmptyTemplate
import org.scalatest.FunSuite

class AboutTest extends FunSuite with TestDatabaseSupport {

  lazy val template = NonEmptyTemplate.load("about").get

  test("Page should render without errors") {
    val html = (new About).attribution(template).toString
    countOccurrences(html, "©") should be > 10
  }
}

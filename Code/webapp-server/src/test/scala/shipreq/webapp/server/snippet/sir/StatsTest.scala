package shipreq.webapp.server.snippet.sir

import shipreq.webapp.server.test.TestDatabaseSupport
import shipreq.webapp.server.util.NonEmptyTemplate
import org.scalatest.FunSuite

class StatsTest extends FunSuite with TestDatabaseSupport {

  lazy val template = NonEmptyTemplate.load("sir/stats").get

  test("Page should render without errors") {
    val html = Stats.render(template).toString
    html should not include(" class=\"err\">")
  }
}

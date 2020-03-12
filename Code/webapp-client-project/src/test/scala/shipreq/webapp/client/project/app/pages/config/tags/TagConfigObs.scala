package shipreq.webapp.client.project.app.pages.config.tags

import japgolly.microlibs.stdlib_ext.StdlibExt._
import org.scalajs.dom.html
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.app.Style

object TagConfigObs {
}

final class TagConfigObs($: DomZipperJs) {
  import TagConfigObs._

  val left = $.child("section", 1 of 2)
  val right = $.child("section", 2 of 2)

  // println(left.outerHTML)
  // println()
  // println(right.outerHTML)
}

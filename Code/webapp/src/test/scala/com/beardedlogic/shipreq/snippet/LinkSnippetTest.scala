package com.beardedlogic.shipreq.snippet

import com.beardedlogic.shipreq.test.TestHelpers
import org.scalatest.FunSpec

class LinkSnippetTest extends FunSpec with TestHelpers {

  it("should render a link to the named page") {
    Link.linkTo("register1")(<div></div>).toString ==== """<a href="/register">Register</a>"""
  }

  it("should throw an exception if page not found") {
    intercept[Exception](Link.linkTo("xcbv"))
  }
}

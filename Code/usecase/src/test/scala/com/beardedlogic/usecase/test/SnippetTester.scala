package com.beardedlogic.usecase
package test

import lib.{SnippetHelpers}
import com.beardedlogic.usecase.util.JavaScriptReaction

class SnippetTester[S <: SnippetHelpers](val snippet: S) extends TestHelpers {
  val js = new JavaScriptReaction
  val mailer = new TestMailer

  snippet.mailer = mailer

  def jsReaction = js.result.toJsCmd

  def assertJsAlert(errorMsg: Option[String]) = {
    TestHelpers.assertJsAlert(js, errorMsg)
    this
  }

  def assertJsErrorNotice(errorMsg: Option[String]) = {
    TestHelpers.assertJsErrorNotice(js, errorMsg)
    this
  }

  def assertEmail(emailFrags: Option[List[String]]) = {
    testListOfZeroOrOne(emailFrags, mailer.sent)(mail =>
      for (f <- emailFrags.get) mail.getContent.toString should include(f)
    )
    this
  }
}

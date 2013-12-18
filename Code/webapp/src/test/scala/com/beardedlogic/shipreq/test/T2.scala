package com.beardedlogic.shipreq.test

import com.beardedlogic.shipreq.app.DI
import com.beardedlogic.shipreq.db.DaoT
import net.liftweb.http.js.JsCmd
import org.mockito.Mockito.{verify, times, never, verifyNoMoreInteractions}
import org.scalatest.Matchers

/**
 * Can't think of what else to call this. It's like Testing 2.0.
 * A better, more descriptive, more reusable approach.
 */
object T2 {

  // ===================================================================================================================
  // Setup

  trait DbSetup {
    def setup(d: DaoT): Unit
  }

  // ===================================================================================================================
  // Expectations: JS

  trait JsExp {
    a =>
    def test(js: JsCmd): Unit
    def &(b: JsExp): JsExp = new JsExp {override def test(js: JsCmd) = { a.test(js); b.test(js) }}
  }

  object NoErrorNotice extends JsExp {
    override def test(js: JsCmd) = TestHelpers.assertJsErrorNotice(js.toJsCmd, None)
  }

  case class JsContains(frag: String, frags: String*) extends JsExp with Matchers {
    override def test(js: JsCmd) = {
      val j = js.toJsCmd
      for (f <- (frag +: frags)) j should include(f)
    }
  }

  case class JsDoesntContain(frag: String, frags: String*) extends JsExp with Matchers {
    override def test(js: JsCmd) = {
      val j = js.toJsCmd
      for (f <- (frag +: frags)) j should not include(f)
    }
  }

  case class HasErrorNotice(frag: String, frags: String*) extends JsExp with Matchers {
    override def test(js: JsCmd) = {
      val j = js.toJsCmd
      for (f <- (frag +: frags)) TestHelpers.assertJsErrorNotice(js.toJsCmd, Some(f))
    }
  }

  // ===================================================================================================================
  // Expectations: Email

  trait EmailExp {
    def test(m: MailTestResult[_]): Unit
  }

  object NoEmailSent extends EmailExp {
    override def test(m: MailTestResult[_]) = m.assertNothingSent()
  }

  case class EmailSentContaining(frag: String, frags: String*) extends EmailExp {
    override def test(m: MailTestResult[_]) = m.assertSent((frag +: frags): _*)
  }

  // ===================================================================================================================
  // Expectations: DB

  trait DbExp {
    final def test(): Unit = DI.DaoProvider.vend.withTransaction(dao => test(dao))
    def test(d: DaoT): Unit

    protected def verifyO[T](mock: T, on: Boolean) = verify(mock, if (on) times(1) else never)
  }

  object NoDbInteraction extends DbExp {
    override def test(d: DaoT) = verifyNoMoreInteractions(d)
  }
}

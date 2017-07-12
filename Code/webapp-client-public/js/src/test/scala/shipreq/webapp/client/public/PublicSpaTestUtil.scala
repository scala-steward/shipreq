package shipreq.webapp.client.public

import japgolly.scalajs.react.test.{MockRouterCtl, ReactTestUtils}
import org.scalajs.dom.html
import scala.annotation.tailrec
import shipreq.base.util.Allow
import shipreq.webapp.base.protocol.ServerSideProc
import shipreq.webapp.base.test.TestClientProtocol
import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.public.spa.{Page, PublicSpa}

object PublicSpaTestUtil {

  val initData = PublicSpaProtocols.InitData(
    allowRegister  = Allow,
    landingPage    = ServerSideProc("landingPage"   , PublicSpaProtocols.LandingPage.Fn),
    register1      = ServerSideProc("register1"     , PublicSpaProtocols.Register.Fn1),
    register2      = ServerSideProc("register2"     , PublicSpaProtocols.Register.Fn2),
    login          = ServerSideProc("login"         , PublicSpaProtocols.Login.Fn),
    resetPassword1 = ServerSideProc("resetPassword1", PublicSpaProtocols.ResetPassword.Fn1),
    resetPassword2 = ServerSideProc("resetPassword2", PublicSpaProtocols.ResetPassword.Fn2))

  class ForTestState {
    val cp       = new TestClientProtocol(false)
    val rc       = MockRouterCtl[Page]()
    var initData = PublicSpaTestUtil.initData

    def render[A](initPage: Page)(f: HtmlDomZipper => A): A = {
      val spa = new PublicSpa(initData, cp)
      ReactTestUtils.withRenderedIntoDocument(spa.Component(PublicSpa.Props(initPage, rc))) { m =>
        f(m.htmlDomZipper)
      }
    }

    def apply(initPage: Page)(f: HtmlDomZipper => Report[String]): Unit =
      render(initPage) { h =>
        val r = f(h)
        assertTestState(r)
        ()
      }
  }

  @tailrec
  def semanticUiDisabled(h: html.Element): Boolean =
    if (h.classList.contains("disabled"))
      true
    else {
      val n = h.parentElement
      if (n eq null)
        false
      else
        semanticUiDisabled(n)
    }
}

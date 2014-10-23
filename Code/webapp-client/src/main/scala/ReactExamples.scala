package hahaa

import org.scalajs.dom._
import scala.scalajs.js
//import scalaz._, Scalaz._
import scalaz.\/
import scalaz.effect.IO
import scalaz.std.option._
import scalaz.syntax.std.option._
import shipreq.webapp.base.protocol.Routines
import shipreq.webapp.client.lib._
import shipreq.webapp.client.ui._
import shipreq.webapp.client.util.route._

object ReactExamples {

  def main(r: Routines.ForCfgReqType) = IO[Unit] {
    example1(document getElementById "eg1")

    manual()

    import shipreq.webapp.client._
    ClientData.init(r.projectInit, clientData => IO {


//      CfgReqTypes.comp(TableIoProps(r.reqTypeCrud, clientData, false)) render document.getElementById("eg2")
//      CfgIncompletions.comp(CfgIncompletions.Props(r.incmpCrud, r.reqTypeImpMod, clientData, false)) render document.getElementById("eg3")
    }).unsafePerformIO()
  }

    // ===================================================================================================================

  import japgolly.scalajs.react._, vdom.ReactVDom._, all._, ScalazReact._

  def manual() = {
    val tgt = document.getElementById("eg2")

    sealed trait MyPage
    object MyPage extends Page[MyPage] {
      val root = Root(RootC)
      val f2 = path("#f2", Route2C)
      val f3 = path("#f3", Route3C)
    }


    def RootC(router: Router[MyPage]): VDom = {
      val c = ReactComponentB[Unit]("RootC")
        .render(_ => {
        div(
          h2("Top Level. Top Secret."),
          div(a(href := "#f2", "F222222222222222222222222", onclick ~~> router.setIO(MyPage.f2))),
          div(a(href := "#f3", "F333333333333333333333333")))
        }).buildU
      c()
    }

    def Route2C(router: Router[MyPage]): VDom = {
      val c = ReactComponentB[Unit]("F2")
        .render(_ => {
        div(
          h3("Cool."),
          div(a("Back", onclick ~~> router.setIO(MyPage.root))))
      }).buildU
      c()
    }

    def Route3C(router: Router[MyPage]): VDom = {
      val c = ReactComponentB[String]("F3")
        .render(p => {
        div(
          h3("Hello ", p),
          div(a("Go back", onclick ~~> router.setIO(MyPage.root))))
      }).build
      c("hehe cool")
    }

    val c = Router.component(BaseUrl("/wip"), MyPage)
    React.renderComponent(c(), tgt)
  }

  def example1(mountNode: Node) = {

    val HelloMessage = ReactComponentB[String]("HelloMessage")
      .render(name => div("Hello ", name))
      .build

    React.renderComponent(HelloMessage("John"), mountNode)
  }
}

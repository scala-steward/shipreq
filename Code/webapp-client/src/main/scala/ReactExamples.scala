package hahaa

import org.scalajs.dom._
import scalaz.effect.IO
import shipreq.webapp.base.protocol.Routines
import shipreq.webapp.client.lib._
import shipreq.webapp.client.ui._

object ReactExamples {

  def main(r: Routines.ForCfgReqType) = IO[Unit] {
    example1(document getElementById "eg1")

    import shipreq.webapp.client._
    ClientData.init(r.projectInit, clientData => IO {
      CfgReqTypes.comp(TableIoProps(r.reqCrud, clientData, false)) render document.getElementById("eg2")
      CfgIncompletions.comp(CfgIncompletions.Props(r.incmpCrud, r.reqImpReq, clientData, false)) render document.getElementById("eg3")
    }).unsafePerformIO()
  }

  // ===================================================================================================================

  import japgolly.scalajs.react._, vdom.ReactVDom._, all._

  def example1(mountNode: Node) = {

    val HelloMessage = ReactComponentB[String]("HelloMessage")
      .render(name => div("Hello ", name))
      .build

    React.renderComponent(HelloMessage("John"), mountNode)
  }
}

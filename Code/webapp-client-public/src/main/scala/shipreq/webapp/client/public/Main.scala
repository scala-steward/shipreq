package shipreq.webapp.client.public

import org.scalajs.dom
import scala.scalajs.js.annotation.JSExportTopLevel
import shipreq.webapp.base.protocol.PublicSpaProtocols
import shipreq.webapp.client.base.protocol.{ClientSideProcImpl, ClientProtocol}

@JSExportTopLevel(PublicSpaProtocols.EntryPointName)
object Main extends ClientSideProcImpl(PublicSpaProtocols.EntryPoint) {

  override def run(i: Unit): Unit = {

//    Styles.addToDocument()

    page.LandingPage.Props().render
      .renderIntoDOM(dom.document.getElementById("tgt"))
  }
}

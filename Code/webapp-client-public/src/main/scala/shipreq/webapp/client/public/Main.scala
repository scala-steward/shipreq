package shipreq.webapp.client.public

import scala.scalajs.js.annotation.JSExportTopLevel
import shipreq.webapp.base.protocol.PublicSpaProtocols
import shipreq.webapp.client.base.protocol.{ClientSideProcImpl, ClientProtocol}

@JSExportTopLevel(PublicSpaProtocols.EntryPointName)
object Main extends ClientSideProcImpl(PublicSpaProtocols.EntryPoint) {

  override def run(i: Unit): Unit = {

//    Styles.addToDocument()

    pages.LandingPage.Props().render.renderIntoDOM(`#root`)
  }
}

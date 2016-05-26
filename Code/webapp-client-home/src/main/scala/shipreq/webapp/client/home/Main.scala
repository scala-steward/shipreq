package shipreq.webapp.client.home

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import scalajs.js.annotation.JSExport
import shipreq.webapp.base.protocol.{ClientFnDecl, InitDataForHomeSpa}
import shipreq.webapp.client.base.protocol.ClientFnImpl

@JSExport(ClientFnDecl.HomeSpaName)
object Main extends ClientFnImpl(ClientFnDecl.HomeSpa) {

  override def run(i: InitDataForHomeSpa): Unit = {
    ReactDOM.render(Home.Component(i), dom.document.getElementById("tgt"))
  }
}


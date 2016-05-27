package shipreq.webapp.client.home

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.protocol.{ClientFnDecl, InitDataForHomeSpa}
import shipreq.webapp.client.base.protocol.ClientFnImpl
import shipreq.webapp.client.home.ui.{Home, Styles}

@JSExport(ClientFnDecl.HomeSpaName)
object Main extends ClientFnImpl(ClientFnDecl.HomeSpa) {

  override def run(i: InitDataForHomeSpa): Unit = {

    Styles.addToDocument()

    ReactDOM.render(
      Home.Component(i),
      dom.document.getElementById("tgt"))
  }
}


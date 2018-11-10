package shipreq.webapp.ssr

import japgolly.scalagraal.Pickled
import japgolly.scalajs.react.ReactDOMServer
import scala.scalajs.js.annotation.JSExportTopLevel
import shipreq.webapp.base.protocol.ClientProtocol
import shipreq.webapp.client.public.PublicSpaProtocols.{InitData => PublicInitData}
import shipreq.webapp.client.public.{Main => PublicMain}

object SsrJs {

  private val cp = ClientProtocol.Noop

  @JSExportTopLevel("public")
  def public(i: Pickled[PublicInitData]): String = {
    val component = PublicMain.component(i.value, cp)
    ReactDOMServer.renderToString(component)
  }
}

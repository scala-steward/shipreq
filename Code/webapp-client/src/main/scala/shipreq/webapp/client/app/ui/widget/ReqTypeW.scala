package shipreq.webapp.client.app.ui.widget

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import shipreq.webapp.base.data._
import shipreq.webapp.client.lib.ui.UI

// TODO ReqTypeW inconsistent style

final case class ReqTypeW(subject       : ReqType.Id,
                  // TODO hoverForDetail: Boolean,
                          project       : Project) {
  @inline def render = ReqTypeW.Component(this)
}

object ReqTypeW {

  // TODO Take project out of props and can have caching
  
  val Component =
    ReactComponentB[ReqTypeW]("ReqType")
      .stateless
      .render((p, _) =>
        UI.must(p.project.reqType(p.subject))(reqtype =>
          <.span(
            ^.title := reqtype.name,
            s"${reqtype.mnemonic.value}")))
      .build
}

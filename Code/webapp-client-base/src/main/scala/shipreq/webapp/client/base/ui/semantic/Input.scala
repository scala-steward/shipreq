package shipreq.webapp.client.base.ui.semantic

import japgolly.scalajs.react.vdom.prefix_<^._

object Input {
  val Action      = divCls("ui action input")
  val ActionError = Action(^.cls := "error")
}

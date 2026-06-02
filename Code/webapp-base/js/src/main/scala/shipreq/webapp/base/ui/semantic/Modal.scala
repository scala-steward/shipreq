package shipreq.webapp.base.ui.semantic

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Modal {

  private var counter = 0

  def nextId(): String = {
    counter += 1
    "semuimodal-" + counter
  }

  // ===================================================================================================================

  /** A modal with static content.
    *
    * Usage:
    *
    * 1. Add `.render` to the root view.
    *    It will be hidden.
    *    It has reusability so as to only evaluate once.
    *
    * 2. Call `.show` to make the modal visible.
    *    This is nearly always going to be in the event-handler of a user action.
    */
  final class Static(val render: VdomElement, val show: Callback)

  def static(header: VdomNode, content: VdomNode): Static = {

    val id = nextId()

    val render: VdomElement =
      <.div(
        ^.id := id,
        ^.cls := "ui long modal",
        Icon.Close.tag,
        <.div(^.cls := "header", header),
        <.div(^.cls := "content", content))

    val component =
      ScalaComponent.builder.static("Modal")(render)
        .componentDidMount($ => Callback(JQuery($.getDOMNode.asElement()).modal()))
        .build

    val show = Callback(JQuery.byId(id).modal("show"))

    new Static(component(), show)
  }
}

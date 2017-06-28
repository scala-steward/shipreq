package shipreq.webapp.client.base.ui.semantic

import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import shipreq.base.util._

object Input {
  val Base        = divCls("ui input")
  val Error       = Base(^.cls := "error")
  val Action      = Base(^.cls := "action")
  val ActionError = Action(^.cls := "error")

  object Text {

    /** Text input with:
      * - icon inside on the left
      */
    def icon(icon: VdomTag, input: VdomTagOf[html.Input], validity: Validity = Valid): VdomTag = {
      var r = Base(^.cls := "left icon", input, icon)
      if (validity is Invalid)
        r = r(^.cls := "error")
      r
    }

    /** Text input with:
      * - icon inside on the left
      * - something (usually a button) attached to the right outside
      */
    def iconAndRightAction(icon: VdomTag, input: VdomTagOf[html.Input], right: TagMod, validity: Validity = Valid): VdomTag = {
      var r = Base(^.cls := "left icon right action", icon, input, right)
      if (validity is Invalid)
        r = r(^.cls := "error")
      r
    }

    def loadingDisabled(value: String, icon: Icon = Icon.Search) =
      Base(^.cls := "loading icon",
        <.input.text(^.value := value, ^.disabled := true),
        icon.tag)
  }

  object Checkbox {

    def apply(input: TagMod, label: TagMod): VdomTag =
      <.div(^.cls := "ui checkbox",
        <.input.checkbox(^.tabIndex := 0, ^.cls := "hidden", input),
        <.label(label))
  }
}

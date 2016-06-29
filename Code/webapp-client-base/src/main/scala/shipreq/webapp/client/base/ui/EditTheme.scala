package shipreq.webapp.client.base.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.UiText

object EditTheme {

  // This probably shouldn't be exposed
  private[this] val editableInline: TagMod =
    TagMod(
      BaseStyles.inlineEdit,
      ^.title := UiText.doubleClickToEdit)

  def editableInline(startEdit: Callback): TagMod =
    editableInline + (^.onDblClick --> startEdit)

  def editableInline(startEdit: Option[Callback]): TagMod =
    startEdit.fold(EmptyTag)(editableInline(_))
}

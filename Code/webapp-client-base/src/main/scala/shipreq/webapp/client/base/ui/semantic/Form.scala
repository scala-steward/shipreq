package shipreq.webapp.client.base.ui.semantic

import japgolly.scalajs.react.vdom.html_<^._

object Form {

  object Field {
    private[Form] val tag =
      <.div(^.className := "field")

    def center: TagMod =
      ^.textAlign.center
  }

  def apply(field1: TagMod, fieldN: TagMod*): VdomTag =
    <.div(^.className := "ui form",
      Field.tag(field1),
      fieldN.toTagMod(Field.tag(_)))

}

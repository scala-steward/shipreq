package shipreq.webapp.client.app.ui

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import scalaz.Equal
import scalaz.effect.IO

object SelectAndInvoke {

  def Component[A: Equal](name: String) =
    ReactComponentB[Props[A]](name)
      .render(render(_))
      .build

  final case class Props[A](selection  : SelectOne.Props[A],
                            buttonLabel: String,
                            invoke     : Option[IO[Unit]],
                            disabled   : Boolean)

  def render[A: Equal](p: Props[A]): ReactTag = {

    val select = {
      // Propagate disabledness
      var q = p.selection
      if (p.disabled && q.onSelect.isDefined)
        q = q.copy(onSelect = None)

      SelectOne.render(q)
    }

    val invokeButton =
      <.button(
        ^.disabled  := (p.disabled || p.invoke.isEmpty),
        ^.onClick ~~>? p.invoke,
        p.buttonLabel)

    <.div(select, invokeButton)
  }
}

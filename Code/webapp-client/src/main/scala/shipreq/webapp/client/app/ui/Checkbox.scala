package shipreq.webapp.client.app.ui

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import japgolly.scalajs.react.extra._
import scalaz.Isomorphism.<=>
import scalaz.effect.IO
import shipreq.webapp.client.lib.{ShowDead, FilterDead}
import shipreq.webapp.client.lib.ui.UI

object Checkbox {

  def apply[A](bool: Boolean <=> A)(set: A => IO[Unit], decor: A => ReactTag => ReactElement) = {
    implicit val reusability = Reusability.by(bool.from)
    ReactComponentB[A]("Checkbox")
      .render { a =>
        val b = bool from a
        val t = UI.checkbox(b)(^.onChange ~~> set(bool.to(!b)))
        decor(a)(t)
      }
      .configure(Reusability.shouldComponentUpdate)
      .build
  }

  def filterDead(set: FilterDead => IO[Unit]) =
    Checkbox(ShowDead)(set, _ => chk => <.label(chk, "Show deleted items."))

  def filterDead_$($: CompStateFocus[FilterDead]): () => ReactElement = {
    val component = filterDead($ setStateIO _)
    () => component($.state)
  }
}
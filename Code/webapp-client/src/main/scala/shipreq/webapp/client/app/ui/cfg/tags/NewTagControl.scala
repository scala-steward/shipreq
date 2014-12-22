package shipreq.webapp.client.app.ui.cfg.tags

import japgolly.scalajs.react._, vdom.prefix_<^.{Tag => ReactTag, Modifier => TagMod, _}, ScalazReact._
import org.scalajs.dom.HTMLSelectElement
import scalaz.effect.IO
import shipreq.webapp.base.data.Tag
import shipreq.webapp.client.WebappClientTmp.WCTmpImplicits._

private[tags] object NewTagControl {

  case class Props(selected: Tag.Type, onChange: Tag.Type => IO[Unit], onCreate: Option[IO[Unit]])

  val Component = ReactComponentB[Props]("NewTagControl")
    .render(p =>
      <.div(
        <.select(
          ^.value := p.selected.key,
          ^.onchange ~~> onchange(p.onChange),
          ^.disabled := p.onCreate.isEmpty,
          Tag.Type.values.map(t => <.option(^.value := t.key, t.name))),
        <.button(
          ^.onclick ~~>? p.onCreate,
          ^.disabled := p.onCreate.isEmpty,
          "Create"
      )))
    .shouldComponentUpdate((c,p,_) => p != c.props)
    .build

  private def onchange(onChange: Tag.Type => IO[Unit]): SyntheticEvent[HTMLSelectElement] => IO[Unit] =
    e => Tag.Type.byKey.get(e.target.value).fold(IO(()))(onChange)
}

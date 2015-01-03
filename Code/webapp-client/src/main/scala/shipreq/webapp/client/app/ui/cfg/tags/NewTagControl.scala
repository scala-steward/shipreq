package shipreq.webapp.client.app.ui.cfg.tags

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import org.scalajs.dom.HTMLSelectElement
import scalaz.effect.IO
import shipreq.webapp.base.data.TagType

private[tags] object NewTagControl {

  case class Props(selected: TagType, onChange: TagType => IO[Unit], onCreate: Option[IO[Unit]])

  val Component = ReactComponentB[Props]("NewTagControl")
    .render(p =>
      <.div(
        <.select(
          ^.value := p.selected.key,
          ^.onChange ~~> onchange(p.onChange),
          ^.disabled := p.onCreate.isEmpty,
          TagType.values.map(t => <.option(^.value := t.key, t.name))),
        <.button(
          ^.onClick ~~>? p.onCreate,
          ^.disabled := p.onCreate.isEmpty,
          "Create"
      )))
    .shouldComponentUpdate((c,p,_) => p != c.props)
    .build

  private def onchange(onChange: TagType => IO[Unit]): SyntheticEvent[HTMLSelectElement] => IO[Unit] =
    e => TagType.byKey.get(e.target.value).fold(IO(()))(onChange)
}

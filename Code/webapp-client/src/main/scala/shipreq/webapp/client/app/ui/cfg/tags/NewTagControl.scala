package shipreq.webapp.client.app.ui.cfg.tags

import scalaz.effect.IO
import shipreq.webapp.base.data.TagType
import shipreq.webapp.base.UiText
import shipreq.webapp.client.app.ui.{SelectInvoke, SelectOne}
import shipreq.webapp.client.util.Enabled
import SelectOne.Choice

private[tags] object NewTagControl {

  val choices = TagType.values.map(tt => Choice(tt, tt.name, Enabled))

  val Component = SelectInvoke.Component[TagType]("NewTag")

  def props(selected: TagType,
            invoke  : Option[IO[Unit]],
            select  : TagType => IO[Unit],
            enabled : Enabled): SelectInvoke.Props[TagType] =

    SelectInvoke.Props(
      SelectOne.Props(selected, choices, Some(select)),
      invoke, UiText.Cfg.startNewButton, enabled)
}

package shipreq.webapp.client.public.pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.base.ui.semantic.{Button, Colour, Icon, Message, Size}
import shipreq.webapp.client.public.Styles.{common => *}

private[pages] object Common {

  def renderTokenInvalid: VdomElement =
    <.div(*.tokenInvalidCont,
      Message(
        Message.Style(Message.Type.Error),
        Icon.Warning,
        "Invalid token",
        "The link emailed to you is no longer valid."))

  def renderTokenExpired = renderTokenInvalid

  def submitButton(title: String, submitCB: Option[Callback]) =
    Button(
      state = Button.State.enabledWhen(submitCB.isDefined),
      colour = Colour.Blue,
      size = Size.Large).tag(
      *.submitButton,
      title,
      ^.onClick -->? submitCB)
}

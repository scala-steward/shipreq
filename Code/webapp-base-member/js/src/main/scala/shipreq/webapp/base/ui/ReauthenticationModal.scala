package shipreq.webapp.base.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{Element, document, html, raw}
import scala.scalajs.js
import scalaz.{-\/, \/, \/-}
import shipreq.base.util.{Allow, Deny, ErrorMsg, Permission}
import shipreq.webapp.base.data.{Disabled, Enabled}
import shipreq.webapp.base.protocol.CommonProtocols.Login
import shipreq.webapp.base.ui.semantic.{Colour, Icon, JQuery, Label, Modal, UsesSemanticUiManually}
import shipreq.webapp.base.user.{PlainTextPassword, Username}

/** Pops up a modal that asks a user to re-authenticate.
  *
  * On success, this modal simply closes. It's expected that the login attempt will make an AJAX call that will update
  * the JWT on success.
  */
@UsesSemanticUiManually
object ReauthenticationModal {

  type AttemptLogin = Login.Request => AsyncCallback[ErrorMsg \/ Permission]

  def apply(username    : Username,
            attemptLogin: AttemptLogin,
            rootDom     : Element = document.body,
            delayMs     : Option[Double] = Some(280)): Modal = {

    val id = Modal.nextId()

    def getDom[N <: raw.Node](sel: String): CallbackTo[N] =
      CallbackTo(rootDom.querySelector(s"#$id $sel").domCast[N])

    val passwordDom    = getDom[html.Input]("input[type=password]")
    val passwordGet    = passwordDom.map(i => PlainTextPassword(i.value))
    val passwordClear  = passwordDom.map(_.value = "")
    val loginButtonDom = getDom[html.Button](".button.primary")
    val errorLabelDom  = getDom[html.Div](".label")

    def setState(form: Enabled, error: Option[ErrorMsg]): Callback =
      for {
        pd <- passwordDom
        lb <- loginButtonDom
        el <- errorLabelDom
      } yield {
        Option(pd).foreach(_.readOnly = form.is(Disabled))
        Option(lb).foreach(_.disabled = form.is(Disabled))
        Option(el).foreach(_.style.display = if (error.isDefined) null else "none")
        Option(el).foreach(_.innerHTML = error.fold("")(_.value))
      }

    val resetForm            = passwordClear >> setState(Enabled, None)
    val modalInitProps       = js.Dynamic.literal(onHidden = resetForm.toJsFn)
    val modalInit            = Callback(JQuery.byId(id).modal(modalInitProps))
    val modalShow            = Callback(JQuery.byId(id).modal("show"))
    val modalHide            = CallbackTo(JQuery(rootDom.querySelector("#" + id)).modal("hide"))
    val errorInvalidPassword = ErrorMsg("Invalid password.")
    val errorLabel           = Label.Style(Label.Type.PointingUp, Colour.Red).div

    val submitAsync: Option[ReactEvent] => AsyncCallback[Unit] = {
      event => {
        val prepare =
          for {
            _ <- event.map(_.preventDefaultCB).getOrEmpty // prevent form submission
            _ <- setState(Disabled, None)
            p <- passwordGet
          } yield Login.Request.validate(-\/(username), p)

        prepare.asAsyncCallback.flatMap {
          case \/-(req) => attemptLogin(req).flatMap {

            case \/-(Allow) =>
              modalHide.asAsyncCallback

            case \/-(Deny) =>
              setState(Enabled, Some(errorInvalidPassword)).asAsyncCallback

            case -\/(err) =>
              setState(Enabled, Some(err)).asAsyncCallback
          }

          case -\/(_) =>
            val a = setState(Enabled, Some(errorInvalidPassword)).asAsyncCallback
            delayMs.fold(a)(a.delayMs) // TODO Remove Option after sjs 1.5.0
        }
      }
    }

    val submit: Option[ReactEvent] => Callback =
      submitAsync(_).toCallback

    val header = "Session Expired"

    val content = TagMod(
      <.p("You must login again to be able to save changes or receive updates."),
      <.form(
        ^.cls := "ui left icon input",
        ^.display.flex,
        <.input.text(
          ^.autoComplete.username,
          ^.display.none,
          ^.readOnly := true,
          ^.value := username.value),
        <.input.password(
          ^.autoComplete.currentPassword,
          ^.autoFocus := true,
          ^.onChange --> setState(Enabled, None),
          UiUtil.submitOnEnter(submit(None))
        ),
        Icon.Lock.tag),
      errorLabel
    )

    val cancelButton =
      <.button(
        ^.cls := "ui button",
        ^.onClick --> modalHide,
        "Cancel")

    val loginButton =
      <.button(
        ^.cls := "ui button primary",
        ^.onClick ==> (e => submit(Some(e))),
        "Login")

    val render: VdomElement =
      <.div(
        ^.id := id,
        ^.cls := "ui mini modal",
        <.div(^.cls := "header", header),
        <.div(^.cls := "content", content),
        <.div(^.cls := "actions", cancelButton, loginButton),
      )

    val component =
      ScalaComponent.builder.static("ReauthModal")(render)
        .componentDidMountConst(modalInit)
        .build

    new Modal(component(), resetForm >> modalShow)
  }
}

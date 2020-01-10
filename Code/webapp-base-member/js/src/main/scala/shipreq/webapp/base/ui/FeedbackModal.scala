package shipreq.webapp.base.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{Element, document, html, raw}
import scala.scalajs.js
import scala.util.Success
import scalaz.{-\/, \/, \/-}
import shipreq.base.util.{ErrorMsg, OpResult}
import shipreq.webapp.base.data.{Disabled, Enabled}
import shipreq.webapp.base.protocol.AjaxClient
import shipreq.webapp.base.protocol.CommonProtocols.SubmitFeedback
import shipreq.webapp.base.ui.semantic.{JQuery, Modal, UsesSemanticUiManually}
import shipreq.webapp.base.util.TextMod

/** Pops up a modal that asks a user for feedback.
  *
  * Usage:
  *
  * 1. Add `.render` to the root view.
  *    It will be hidden.
  *    It has reusability so as to only evaluate once.
  *
  * 2. Call `.run` to display the modal.
  */
final case class FeedbackModal(render: VdomElement, run: AsyncCallback[OpResult])

@UsesSemanticUiManually
object FeedbackModal {

  type SubmitFn = SubmitFeedback.UserInput => AsyncCallback[ErrorMsg \/ Unit]

  private[ui] val errorEmptyFeedback = ErrorMsg("You can't submit nothing as your feedback.")

  def apply(metadata: CallbackTo[SubmitFeedback.Metadata]): FeedbackModal =
    apply(metadata, AjaxClient.Binary)

  def apply(metadata: CallbackTo[SubmitFeedback.Metadata], ajaxClient: AjaxClient.Binary): FeedbackModal = {
    import SubmitFeedback._
    val f = ajaxClient.invoker(ajax).contramapInputCB((i: UserInput) => metadata.map(Request(i, _)))
    apply(f(_), document.body)
  }

  def apply(submitFeedback: SubmitFn,
            rootDom       : Element): FeedbackModal = {

    val id = Modal.nextId()

    var onCompletion: Callback = Callback.empty
    var lastResult: OpResult = OpResult.Failure

    def getDom[N <: raw.Node](sel: String): CallbackTo[N] =
      CallbackTo(rootDom.querySelector(s"#$id $sel").domCast[N])

    val feedbackDom     = getDom[html.Input]("textarea")
    val feedbackGet     = feedbackDom.map(i => TextMod.multiLineWhitespace(i.value))
    val feedbackClear   = feedbackDom.map(_.value = "")
    val loginButtonDom  = getDom[html.Button](".button.primary")
    val errorMessageDom = getDom[html.Div](".ui.message")

    def setState(form: Enabled, error: Option[ErrorMsg], inFlight: Boolean): Callback =
      for {
        fd <- feedbackDom
        lb <- loginButtonDom
        em <- errorMessageDom
      } yield {
        for (d <- Option(fd)) {
          d.readOnly = form.is(Disabled)
        }
        for (d <- Option(em)) {
          d.style.display = if (error.isDefined) null else "none"
          d.innerHTML = error.fold("")(_.value)
        }
        GeneralTheme.nonReact.setStateOfSubmitButton(lb)(form, inFlight = inFlight)
      }

    val resetForm      = feedbackClear >> setState(Enabled, None, inFlight = false)
    val onHide         = resetForm >> Callback.byName(onCompletion)
    val modalInitProps = js.Dynamic.literal(onHidden = onHide.toJsFn)
    val modalInit      = Callback(JQuery.byId(id).modal(modalInitProps))
    val modalShow      = Callback(JQuery.byId(id).modal("show"))
    val modalHide      = CallbackTo(JQuery(rootDom.querySelector("#" + id)).modal("hide"))
    val errorMessage   = <.div(^.cls := "ui message error")

    val submitAsync: Option[ReactEvent] => AsyncCallback[Unit] =
      event => {
        val prepare: CallbackTo[SubmitFeedback.UserInput] =
          for {
            _ <- event.map(_.preventDefaultCB).getOrEmpty // prevent form submission
            _ <- setState(Disabled, None, inFlight = true)
            f <- feedbackGet
          } yield SubmitFeedback.UserInput(f)

        prepare.asAsyncCallback.flatMap { input =>
          if (input.feedback.isEmpty)
            setState(Enabled, Some(errorEmptyFeedback), inFlight = false).asAsyncCallback
          else
            submitFeedback(input).flatMap {
              case \/-(_) =>
                (Callback {lastResult = OpResult.Success} >> modalHide).asAsyncCallback
              case -\/(err) =>
                setState(Enabled, Some(err), inFlight = false).asAsyncCallback
            }
        }
      }

    val submit: Option[ReactEvent] => Callback =
      submitAsync(_).toCallback

    val header = "Send Feedback"

    val content = TagMod(
      <.p("Thank you for taking a moment to give us feedback."),
      <.p("Whether it be a suggestion for improvement, a bug report, or even just sharing your experience, all feedback is appreciated."),
      <.div(
        ^.cls := "ui form",
        ^.paddingTop :="0.3333em",
        <.div(
          ^.cls := "field",
          <.textarea(
            ^.autoFocus := true,
            ^.placeholder := "What would you like to say?",
            ^.rows := 12,
            ^.onChange --> setState(Enabled, None, inFlight = false),
            GeneralTheme.submitOnCtrlEnter(submit(None))))),
      errorMessage)

    val cancelButton =
      <.button(
        ^.cls := "ui button",
        ^.onClick --> modalHide,
        "Cancel")

    val loginButton =
      <.button(
        ^.cls := "ui button primary",
        ^.onClick ==> (e => submit(Some(e))),
        "Send")

    val render: VdomElement =
      <.div(
        ^.id := id,
        ^.cls := "ui modal",
        <.div(^.cls := "header", header),
        <.div(^.cls := "content", content),
        <.div(^.cls := "actions", cancelButton, loginButton),
      )

    val component =
      ScalaComponent.builder.static("FeedbackModal")(render)
        .componentDidMountConst(modalInit)
        .build

    val run: AsyncCallback[OpResult] = {
      val start: CallbackTo[AsyncCallback[OpResult]] =
        for {
          (p, complete) <- AsyncCallback.promise[OpResult]
          _             <- Callback {
                             lastResult = OpResult.Failure
                             onCompletion = CallbackTo(lastResult).flatMap(p => complete(Success(p)))
                           }
          _             <- resetForm
          _             <- modalShow
        } yield p

      for {
        promise <- start.asAsyncCallback
        result  <- promise
      } yield result
    }

    FeedbackModal(component(), run)
  }

  implicit val reusability: Reusability[FeedbackModal] =
    Reusability.byRef
}

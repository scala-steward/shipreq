package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react.ReactMonocle._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import scalacss.ScalaCssReact._
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.feature.{AsyncFeature, EditorStatus}
import shipreq.webapp.base.protocol.ServerSideProcInvoker
import shipreq.webapp.base.ui.semantic.{Button, Colour, ColourPlus, Icon, Message, Size}
import shipreq.webapp.client.project.app.Style.{statusPage => *}
import shipreq.webapp.client.project.widgets.ProjectWidgets
import shipreq.webapp.client.project.widgets.editors_with_controls.RichTextEditor
import shipreq.webapp.member.feature.{EditControlsFeature, PreviewFeature}
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.data.derivation.NaTags
import shipreq.webapp.member.project.protocol.websocket.UpdateLivenessCmd
import shipreq.webapp.member.project.text.TextSearch

object DeleteProjectForm {

  final case class Props(projectName      : String,
                         project          : Project,
                         widgets          : ProjectWidgets.NoCtx,
                         textSearch       : TextSearch,
                         state            : StateSnapshot[State],
                         close            : Callback,
                         sspUpdateLiveness: ServerSideProcInvoker[UpdateLivenessCmd.Delete, ErrorMsg, Any],
                         async            : AsyncFeature.ReadWrite.D0[ErrorMsg],
                        ) {
    @inline def render: VdomElement = Component(this)
  }

  @Lenses
  final case class State(reason: String)

  object State {
    def init: State = State("")
  }

  private def render(p: Props) = {

    val heading = Message(
      Message.Style(colour = Colour.Black, size = Size.Huge),
      Icon.Trash,
      "You are about to delete the project:",
      <.span(*.deleteFormHeaderProjectName, p.projectName)
    )

    val inFlight: Boolean =
      p.async.isInProgress

    val editorProps =
      RichTextEditor.DeletionReason.Optional(
        project            = p.project,
        naTags             = NaTags.none,
        plainTextNoCtx     = p.widgets.plainText,
        textSearch         = p.textSearch,
        projectWidgets     = p.widgets,
        edit               = p.state.zoomStateL(State.reason),
        asyncStatus        = EditorStatus.async(p.async.read),
        abort              = None,
        abortVerb          = "",
        abortConfirmation  = None,
        autoFocus          = true,
        commitFn           = None,
        commitVerb         = "",
        editorStyle        = EditControlsFeature.Style.default,
        preview            = PreviewFeature.ReadWrite.Single.alwaysShow,
        preEditValue       = None,
        extraControls      = EditControlsFeature.ExtraControls.empty,
        showInstructions   = true,
        optionalFullscreen = None)

    val editor = RichTextEditor.DeletionReason.Component(editorProps)

    val cancel =
      Button(
        tipe   = Button.Type.BasicIconAndText(Icon.Remove, "Cancel"),
        state  = Button.State.enabledWhen(!inFlight),
        colour = Colour.Black,
      ).tag(^.onClick --> p.close)

    val buttonGap = <.span(*.deleteFormButtonGap)

    val delete = {
      val cmd = UpdateLivenessCmd.Delete(editorProps.richText)
      val onClick = p.async.write.onFailureShowAndForget(
        p.sspUpdateLiveness(cmd).flatTap {
          case \/-(_) => p.close.asAsyncCallback
          case -\/(_) => AsyncCallback.unit
        })
      Button(
        tipe   = Button.Type.BasicIconAndText(Icon.Trash, "Delete"),
        state  = Button.State.loadingWhen(inFlight),
        colour = ColourPlus.Negative,
      ).tag(^.onClick --> onClick)
    }

    <.div(
      heading,
      <.div(*.deleteFormEditorHeader, "Deletion Reason:"),
      editor,
      <.div(*.deleteFormButtons, cancel, buttonGap, delete),
    )
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .build
}

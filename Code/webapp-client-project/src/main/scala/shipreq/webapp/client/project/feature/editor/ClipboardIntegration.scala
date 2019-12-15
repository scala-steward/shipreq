package shipreq.webapp.client.project.feature.editor

import japgolly.scalajs.react._
import org.scalajs.dom.ext.KeyCode
import Feature.ReadWrite
import shipreq.webapp.base.feature.AsyncFeature
import shipreq.webapp.base.feature.clipboard.{Clipboard, ClipboardData}
import shipreq.webapp.base.util.Browser

object ClipboardIntegration {

  def keys[A](editor: ReadWrite.ForEditor[A, Any])(a: A)(e: ReactKeyboardEventFromHtml): CallbackOption[Unit] =
    Browser.cmdOrCtrlKeyCodeSwitch(e) {
      case KeyCode.V => onPaste(editor)(a)(e)
    }

  def onPaste[A](editor: ReadWrite.ForEditor[A, Any])(a: A)(e: ReactEventFromHtml): Callback = {

    def attemptPaste(cd: ClipboardData, pd: PasteDecision): Callback = {
      def wrap(paste: Option[Callback]): Callback =
        paste match {
          case Some(p) => e.preventDefaultCB >> p
          case None    => Callback.empty
        }

      pd match {
        case PasteDecision.Replace =>
          wrap(editor.paste(cd))

        case PasteDecision.OpenAndReplace =>
          wrap(editor.withClipboardData(cd).startEdit)

        case PasteDecision.Ignore =>
          Callback.empty
      }
    }

    val async =
      for {
        _  <- AsyncCallback.point(e.persist())
        cd <- Clipboard.read
        pd <- PasteDecision(editor).asAsyncCallback
        _  <- attemptPaste(cd, pd).asAsyncCallback
      } yield ()

    async
      .toCallback
      .when_(e.target == e.currentTarget)
      .unless_(e.defaultPrevented)
  }

  private sealed trait PasteDecision

  private object PasteDecision {
    case object Ignore         extends PasteDecision
    case object Replace        extends PasteDecision
    case object OpenAndReplace extends PasteDecision

    def apply(editor: ReadWrite.ForAnyEditor): CallbackTo[PasteDecision] =
      CallbackTo {
        if (editor.read.isOpen)
          editor.asyncState match {
            case None                                      => PasteDecision.Replace
            case Some(AsyncFeature.Status.Failed(_, _, _)) => PasteDecision.Replace
            case Some(AsyncFeature.Status.InProgress)      => PasteDecision.Ignore
          }
        else
          PasteDecision.OpenAndReplace
      }
  }
}

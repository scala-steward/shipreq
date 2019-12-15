package shipreq.webapp.client.project.feature.editor

import japgolly.microlibs.stdlib_ext.StdlibExt._
import japgolly.scalajs.react._
import org.scalajs.dom.ext.KeyCode
import shipreq.webapp.base.lib.DomUtil._
import Feature.ReadWrite

object EditorKeys {

  def apply[A](editor: ReadWrite.ForEditor[A, Any])(a: A)(e: ReactKeyboardEventFromHtml): CallbackOption[Unit] = {

    def handlers: CallbackOption[Unit] =
      CallbackOption.keyCodeSwitch(e) {
        case KeyCode.F2 => focusOrStartEditor(editor, e)
      } | ClipboardIntegration.keys(editor)(a)(e)

    (CallbackOption.require(doesEventTargetCell(e)) >> handlers).asEventDefault(e)
  }

  private def focusOrStartEditor(editor: ReadWrite.ForAnyEditor, event: ReactEventFromHtml): CallbackOption[Unit] =
    if (editor.read.isOpen)
      focusChild(event)
    else
      editor.startEdit.getOrEmpty

  private def focusChild(event: ReactEventFromHtml): CallbackOption[Unit] =
    CallbackOption
      .liftOption(focusableChildren(event.currentTarget.domAsHtml).nextOption())
      .map(_.focus())

}

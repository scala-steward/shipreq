package shipreq.webapp.base.feature.clipboard

import japgolly.scalajs.react.AsyncCallback

object TestClipboard {

  private final class Mock extends Clipboard {
    var text = ""
    override val read = AsyncCallback.point(ClipboardData(text))
    override def write(d: ClipboardData) = AsyncCallback.point{text = d.text}
  }

  private val mock = new Mock

  Clipboard.setClipboardImpl(mock)

  def writeText(t: String): Unit =
    mock.text = t
}

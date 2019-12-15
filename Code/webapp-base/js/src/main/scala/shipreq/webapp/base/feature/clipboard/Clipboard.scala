package shipreq.webapp.base.feature.clipboard

import japgolly.scalajs.react._
import scala.scalajs.js

object Clipboard {

  private lazy val clipboard: Option[AsyncCallback[ClipboardJs]] =
    ClipboardJs.instance.toOption.map(AsyncCallback.pure)

  private def async[A](f: ClipboardJs => js.Promise[A]): Option[AsyncCallback[A]] =
    clipboard.map(_.flatMap(c => AsyncCallback.fromJsPromise(f(c))))

  lazy val readText: Option[AsyncCallback[String]] =
    async(_.readText())

  lazy val read: AsyncCallback[ClipboardData] =
    readText
      .getOrElse(AsyncCallback.pure(""))
      .map(Option(_).getOrElse(""))
      .map(ClipboardData.apply)

//  def read(event: ReactClipboardEvent): AsyncCallback[ClipboardData] =
//    readText
//      .getOrElse(AsyncCallback.pure(event.clipboardData.getData("text/plain")))
//      .map(Option(_).getOrElse(""))
//      .map(ClipboardData)

  def writeText(s: String): AsyncCallback[Unit] =
    AsyncCallback.sequenceOption(async(_.writeText(s))).void

//  lazy val read: Option[AsyncCallback[DataTransfer]] =
//    async(_.read()).map(_.map(new DataTransfer(_)))
//
//  final class DataTransfer(private val raw: ClipboardJs.DataTransfer) extends AnyVal {
//    def setData(`type`: String, value: String): Callback                   = Callback(raw.setData(`type`, value))
//    def getData(`type`: String)               : CallbackTo[Option[String]] = CallbackTo(raw.getData(`type`).toOption)
//  }

}

// I'll improve this later to handle more than just text
final case class ClipboardData(text: String)

object ClipboardData {
  implicit def reusability: Reusability[ClipboardData] =
    Reusability.derive
}

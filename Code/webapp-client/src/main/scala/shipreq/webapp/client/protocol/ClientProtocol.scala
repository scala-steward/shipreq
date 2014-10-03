package shipreq.webapp.client.protocol

import org.scalajs.dom.console
import scala.scalajs.js
import scalaz.effect.IO
import scalaz.{-\/, \/-, \/}
import upickle._
import shipreq.webapp.shared.protocol.Routine

object ClientProtocol {

  def parseJsObject[T: Reader](a: js.Any): Throwable \/ T =
    try
      \/-(readJs[T](json.readJs(a)))
    catch {
      case e: Throwable => -\/(e)
    }

  def jsonEffect[T: Reader](f: T => IO[Unit]): js.Any => Unit =
    a => parseJsObject[T](a) match {
      case \/-(b) => f(b).unsafePerformIO()
      case -\/(e) => handleJsonParsingError(e)
    }

  private def handleJsonParsingError(e: Throwable): Unit = () // TODO log unless release mode

  def readCluster[G <: Routine.Group : Reader](a: js.Any) = // TODO rename
    parseJsObject[G](a)

  def call[D <: Routine.Desc](r: Routine.Remote[D])(input: r.d.I, success: r.d.O => IO[Unit], f: FailureIO): IO[Unit] = {
    import r.d.{wi, ro}
    val i = js.encodeURIComponent(write(input))
    val q = s"${r.n}=$i"
    val s = jsonEffect[r.d.O](success)
    val ff = () => f.io.unsafePerformIO() // TODO log unless release mode
    IO(LiftAjax.lift_ajaxHandler(q, s, ff, "json"))
  }
}

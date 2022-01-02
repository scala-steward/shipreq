package shipreq

import scala.scalajs.js

// JS
object Predef extends PredefShared {

  @inline def console = org.scalajs.dom.console

  @inline def JSON = js.JSON

  @inline def BREAKPOINT() = js.special.debugger()

  def setStackTraceLimit(lines: Int): Unit =
    js.constructorOf[js.Error].stackTraceLimit = lines

  override implicit def predefExtString(s: String): AnyVal with PredefShared.ExtString =
    new PredefJs.ExtString(s)
}

object PredefJs {
  import java.lang.String

  final class ExtString(private val s: String) extends AnyVal with PredefShared.ExtString {
    override def quote =
      scala.scalajs.js.JSON.stringify(s)
  }
}

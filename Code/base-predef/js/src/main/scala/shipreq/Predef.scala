package shipreq

// JS
object Predef extends PredefShared {

  @inline def console = org.scalajs.dom.console

  @inline def JSON = scala.scalajs.js.JSON
}

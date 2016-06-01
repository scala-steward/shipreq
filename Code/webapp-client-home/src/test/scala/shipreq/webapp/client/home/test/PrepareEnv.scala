package shipreq.webapp.client.home.test

object PrepareEnv {
  def apply(): Unit = ()

  // Initialise styles
  shipreq.webapp.client.home.ui.Styles

  // console.error is undefined by Scala.JS due to PhantomJS being a piece of shit
  def console = scalajs.js.Dynamic.global.console
  console.error = console.info
}

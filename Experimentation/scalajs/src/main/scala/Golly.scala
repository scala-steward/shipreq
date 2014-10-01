package golly

import scala.scalajs.js

import japgolly.scalajs.react._
import vdom.ReactVDom._
import all._
import ScalazReact._

import scala.scalajs.js.annotation.JSExportAll
import org.scalajs.dom.{HTMLInputElement, console, document, window, Node}

@JSExportAll
object Golly extends js.JSApp {

  case class State(secondsElapsed: Long)

  class Backend {
    var interval: js.UndefOr[Int] = js.undefined
    def tick(scope: ComponentScopeM[_, State, _]): js.Function =
      () => scope.modState(s => State(s.secondsElapsed + 1))
  }

  /*
   * This is the callback logic for an external event. Static. Testable in isolation.
   */
  def callbackS(str: String) = ReactS.mod[State](s => {
    val i = str.replaceAll("[^0-9]","").toInt
    State(s.secondsElapsed + i)
  })

  val Timer = ReactComponentB[String]("Timer")
    .initialState(State(0))
    .backend(_ => new Backend)
    .render((P,S,_) => div(p("Prop: ",P), p("Seconds elapsed: ", S.secondsElapsed)))
    .componentDidMount(scope => {
      scope.backend.interval = window.setInterval(scope.backend.tick(scope), 1000)

     /*
      * Here we wire up the component to listen to external events.
      */
      val cb: String => Unit = scope runState callbackS(_) unsafePerformIO()
      listeners ::= cb

      /* Test referencial equality for callback unregistration */
      val cb2: String => Unit = scope runState callbackS(_) unsafePerformIO()
      console.log(s"cb == cb: ", cb == cb, s"| cb == cb2: ", cb == cb2)
      console.log(s"cb eq cb: ", cb eq cb, s"| cb eq cb2: ", cb eq cb2)
    })
    .componentWillUnmount(_.backend.interval foreach window.clearInterval)
    .create

  var x: js.UndefOr[ComponentScopeM[String, _, _]] = js.undefined

  /*
   * External events broadcast here.
   */
  var listeners: List[String => Unit] = Nil
  def broadcast(s: String): Unit = listeners foreach (_(s))

  def main(): Unit = {
    x = React.renderComponent(Timer("init"), document getElementById "target1")
    println("TRY THIS: golly.Golly().broadcast('Add 100')")
  }

}

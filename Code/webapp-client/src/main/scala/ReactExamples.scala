package hahaa

import shipreq.webapp.shared.Interface

import scala.scalajs.js
import org.scalajs.dom.{document, window, Node, console, alert}

import scala.scalajs.js.annotation.{JSExport, JSName}

@JSName("liftAjax")
object LiftAjax extends js.Object {

  def lift_ajaxHandler(input: String,
                       success: js.Function1[js.Any, Unit] = null,
                       failure: js.Function0[Unit] = null,
                       respType: String = null): Boolean = ???
}

object ReactExamples extends js.JSApp {

  override def main(): Unit = {
    example1(document getElementById "eg1")
    example2(document getElementById "eg2")
  }

  @JSExport
  def wire(a: String, b: String) = Interface.Page.WIP(
    Interface.Wired(a, Interface.Defn.Square),
    Interface.Wired(b, Interface.Defn.Half))

  @JSExport
  def invokeSquare(p: Interface.Page.WIP, n: js.Number): Unit =
    invokeCallback(p.square)(n.toInt, s => alert(s"RESPONSE: [$s]"))

//  @js.annotation.JSExport
//  def invokeSquare(fn: String, n: js.Number): Unit = {
//    val XXX = CallbackFnNamesForPageX(RemoteCallback(fn, CallbackDefs.Square), null)
//    invokeCallback(XXX.square)(n.toInt, s => alert(s"$n² = $s"))
//  }

//  @js.annotation.JSExport
//  def invokeHalf(fn: String, n: js.Number): Unit = {
//    val XXX = CallbackFnNamesForPageX(null, RemoteCallback(fn, CallbackDefs.Half))
//    invokeCallback(XXX.half)(n.toInt, s => alert(s"$n/2 = $s"))
//  }

  def invokeCallback[C <: Interface.Defn[I, O], I, O](r: Interface.Wired[C, I, O])(i: I, cb: O => Unit): Unit = {
    val ii = js.encodeURIComponent(r.c serialise i)
    val s: js.Any => Unit = o => cb(o.asInstanceOf[O]) // TODO
    // needs failure
    LiftAjax.lift_ajaxHandler(s"${r.fn}=$ii", s, null, "json")
  }

  // ===================================================================================================================

  import japgolly.scalajs.react._
  import vdom.ReactVDom._
  import all._

  def example1(mountNode: Node) = {

    val HelloMessage = ReactComponentB[String]("HelloMessage")
      .render(name => div("Hello ", name))
      .create

    React.renderComponent(HelloMessage("John"), mountNode)
  }

  // ===================================================================================================================

  def example2(mountNode: Node) = {

    case class State(secondsElapsed: Long)

    class Backend {
      var interval: js.UndefOr[Int] = js.undefined
      def tick(scope: ComponentScopeM[_, State, _]): js.Function =
        () => scope.modState(s => State(s.secondsElapsed + 1))
    }

    val Timer = ReactComponentB[Unit]("Timer")
      .initialState(State(0))
      .backend(_ => new Backend)
      .render((_,S,_) => div("Seconds elapsed: ", S.secondsElapsed))
      .componentDidMount(scope =>
      scope.backend.interval = window.setInterval(scope.backend.tick(scope), 1000))
      .componentWillUnmount(_.backend.interval foreach window.clearInterval)
      .createU

    React.renderComponent(Timer(), mountNode)
  }

}

package shipreq.webapp.client.util

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import scalajs.js
import org.scalajs.dom.{document, Event, KeyboardEvent}
import org.scalajs.dom.ext.KeyCode
import scala.collection.immutable.BitSet
import scalaz.effect.IO

object EventListener {

  /**
   *
   * @param eventType A string representing the
   *                  <a href="https://developer.mozilla.org/en-US/docs/DOM/event.type">event type</a> to listen for.
   * @param useCapture If true, useCapture indicates that the user wishes to initiate capture.
   *                   After initiating capture, all events of the specified type will be dispatched to the registered
   *                   listener before being dispatched to any EventTarget beneath it in the DOM tree.
   *                   Events which are bubbling upward through the tree will not trigger a listener designated to use
   *                   capture.
   */
  def install[P, S, B <: OnUnmount, E <: Event](eventType: String,
                                                listener: ComponentScopeM[P, S, B] => E => Unit,
                                                useCapture: Boolean = false) =
    OnUnmount.install compose ((_: ReactComponentB[P, S, B])
      .componentDidMount($ => {
      val fe = listener($)
      val f1: js.Function1[E, Unit] = (e: E) => fe(e)
      val f2 = f1.asInstanceOf[js.Function1[Event, Unit]] // TODO Workaround for scala-js-dom 0.8.0
      document.addEventListener(eventType, f1, useCapture)
      $.backend.onUnmount(document.removeEventListener(eventType, f2, useCapture))
    }))


  def installIO[P, S, B <: OnUnmount, E <: Event](eventType: String,
                                                listener: ComponentScopeM[P, S, B] => E => IO[Unit],
                                                useCapture: Boolean = false) =
    install[P, S, B, E](eventType, $ => { val f = listener($); f(_).unsafePerformIO() }, useCapture)
}


object KeyPressListener {

  def install[P, S, B <: KeyPressListener](useCapture: Boolean = false) =
    EventListener.install[P,S,B,KeyboardEvent]("keydown", _.backend.onKeyDown, useCapture) compose
    EventListener.install[P,S,B,KeyboardEvent]("keyup",   _.backend.onKeyUp,   useCapture)

  import scalaz.effect.{IO => IOZ}

  val nopIO = IOZ(())

  trait IO extends KeyPressListener {
    override final def onKeyPress(keyCode: Int) = onKeyPressIO(keyCode).unsafePerformIO()
    val onKeyPressIO: Int => IOZ[Unit]

    def matchNoMods(pf: PartialFunction[Int, IOZ[Unit]]): Int => IOZ[Unit] =
      i => if (modsDown.isEmpty) pf.applyOrElse(i, (_: Int) => nopIO) else nopIO
  }

  val modKeys: BitSet =
    BitSet(KeyCode.alt, KeyCode.ctrl, KeyCode.shift,
      91, 92, 93, 224, // OSLeft & OSRight
      225)             // AltGraph
}

trait KeyPressListener extends OnUnmount {

  private[this] var _keysDown = BitSet.empty
  private[this] var _modsDown = BitSet.empty

  final def modsDown = _modsDown
  final def keysDown = _keysDown

  @inline private def _keychange(e: KeyboardEvent)(f: (BitSet, Int) => BitSet): Int = {
    val i = e.keyCode
    if (KeyPressListener.modKeys contains i)
      _modsDown = f(_modsDown, i)
    else
      _keysDown = f(_keysDown, i)
    i
  }

  def onKeyDown(e: KeyboardEvent): Unit = {
    val i = _keychange(e)(_ + _)
    onKeyPress(i)
  }

  def onKeyUp(e: KeyboardEvent): Unit = {
    _keychange(e)(_ - _)
  }

  def onKeyPress(keyCode: Int): Unit

//  def singleKeyDown: Int =
//    if (_keysDown.size == 1)
//      _keysDown.head
//    else
//      -1
}


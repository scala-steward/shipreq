package shipreq.webapp.client.util.ui.tablespec2

import japgolly.scalajs.react._, vdom.ReactVDom.{Tag => _, _}, all._, ScalazReact._
import monocle._
import shipreq.webapp.base.validation._
import shipreq.webapp.client.util.ui.Util._
import scalaz.effect.IO
import scalajs.js.undefined
import scalaz._, Scalaz._

object Neo {

  // VP A B → (S → B → VR C) → (S → VP A C)
  // ------   --------------   ++++++++++++
  //           +   +      -     -      +i+i

  class externalValidation[S, A, B, C](
//    vp: ValidationPart[A, B],
    f: (S, B) => ValidationResult[C]
  ,w: Int => S
  ,x: S => Int
                                        ) {

    def apply: S => ValidationPart[A, C] =
      s => {
      val x = f(s, ???)
      ???
    }

    def maps[T](g: T => S, h: S => T) =
      new externalValidation[T, A, B, C](
        (t, b) => {
          val s: S = g(t)
          f(s, b)
        }
      ,i => h(w(i))
      ,t => x(g(t))
      )
  }

  // Args
  // - for each arg, measure variance of each type
  // - variance will determine what is needed to shift type later (functor variance)

  // Functions
  // - in & out of each type determine subtype variance of class's type members


  case class EditorCallbacks[-B, -C, +D](onChange: (B, C) => D,
                                     onCancel: C => D,
                                     onEditFinished: C => D) {

    def mapB[X](f: X => B): EditorCallbacks[X, C, D] = copy[X, C, D](onChange = (a,c) => onChange(f(a), c))
    def mapC[X](f: X => C): EditorCallbacks[B, X, D] = EditorCallbacks[B, X, D]((a,x) => onChange(a,f(x)), onCancel compose f, onEditFinished compose f)
    def mapD[X](f: D => X): EditorCallbacks[B, C, X] = EditorCallbacks[B, C, X]((a,c) => f(onChange(a,c)), f compose onCancel, f compose onEditFinished)
  }

  case class EditorInput[+A, -B, -C, +D](data: A,
                                     cssClass: String,
                                     editable: Option[EditorCallbacks[B, C, D]]) {

//    def mapInput[X](f: A => X)       : EditorInput[X, B, C, D] = copy(data = f(data))
//    def contramapOutput[X](f: X => B): EditorInput[A, X, C, D] = copy(editable = editable.map(_ contramap f))
    def mapC[X](f: X => C)           : EditorInput[A, B, X,  D] = copy(editable = editable.map(_ mapC f))
    def mapD[X](f: D => X)           : EditorInput[A, B, C, X ] = copy(editable = editable.map(_ mapD f))
//    def dimap[X, Y](f: A => X, g: Y => B): EditorInput[X, Y, C] =
//      copy(data = f(data), editable = editable.map(_ contramap g))
  }

  case class Editor[-A, +B, +C, -D, +V](render: EditorInput[A, B, C, D] => V) {
//    def contramapInput[X](f: X => A): Editor[X, B, C, V] = Editor(i => render(i mapInput f))
//    def mapOutput[X](f: B => X): Editor[A, X, C, V] = Editor(i => render(i contramapOutput f))
    def mapC[X](f: C => X): Editor[A, B, X, D, V] = Editor(i => render(i mapC f))
    def mapD[X](f: X => D): Editor[A, B, C, X, V] = Editor(i => render(i mapD f))
  }

  type RU = ReactST[IO, Unit, Unit]
  val RU = ReactS.FixT[IO, Unit]
  val nopRU = RU.ret(())

  def textEditor(node: Tag): Editor[String, String, RU, IO[Unit], Modifier] =
    Editor(ei => {
      val base = node(cls := ei.cssClass, value := ei.data)
      ei.editable match {
        case None =>
          base(readonly := true)
        case Some(cb) =>
          base(
            onchange  ~~> textChangeRecv(cb.onChange(_, nopRU)),
            onkeydown ~~> cb.onCancel.compose(cancelOnEscape),
            onblur    ~~> cb.onEditFinished(nopRU))
      }
    })

  def cancelOnEscape: ReactKeyboardEventH => RU =
    e => e.key match {
      case "Escape" => // TODO use KeyValue
        val t = e.target
        RU.callbackM(IO(t.blur()), e.preventDefaultIO >> e.stopPropagationIO)
      case _ =>
        nopRU
    }

  type S = Int
  type T = List[Int]
  val e1 = textEditor(input)
  val e2 = e1.mapC(_.zoomU[S])

  val F: ComponentStateFocus[T] = ???
  val e3 = e2.mapC(_.zoom2[T](_.head, (a,b) => b :: a))
  type ST = ReactST[IO, T, Unit]
  val cbs = EditorCallbacks[String, ST, IO[Unit]](
    (i,st) => F.runState(st >> updateState(i)),
    st => F.runState(st),
    st => F.runState(st >> validateSaveLockRow))

  def updateState(i: String): ST = ???
  def validateSaveLockRow: ST = ???
}

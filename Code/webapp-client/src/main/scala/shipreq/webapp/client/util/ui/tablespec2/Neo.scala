package shipreq.webapp.client.util.ui.tablespec2

import japgolly.scalajs.react._, vdom.ReactVDom.{Tag => _, _}, all._, ScalazReact._
import monocle._
import shipreq.webapp.base.validation._
import shipreq.webapp.client.util.ui.Util._
import scala.util.Try
import scalaz.effect.IO
import scalajs.js.undefined
import scalaz._, Scalaz._
import shipreq.base.util.ScalaExt._

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

  // ===================================================================================================================

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

    def mapA[X](f: A => X): EditorInput[X, B, C, D] = copy(data = f(data))
    def mapB[X](f: X => B): EditorInput[A, X, C, D] = copy(editable = editable.map(_ mapB f))
    def mapC[X](f: X => C): EditorInput[A, B, X, D] = copy(editable = editable.map(_ mapC f))
    def mapD[X](f: D => X): EditorInput[A, B, C, X] = copy(editable = editable.map(_ mapD f))
//    def dimap[X, Y](f: A => X, g: Y => B): EditorInput[X, Y, C] =
//      copy(data = f(data), editable = editable.map(_ contramap g))
  }

  case class Editor[-A, +B, +C, -D, +V](render: EditorInput[A, B, C, D] => V) {
    def mapA[X](f: X => A): Editor[X, B, C, D, V] = Editor(i => render(i mapA f))
    def mapB[X](f: B => X): Editor[A, X, C, D, V] = Editor(i => render(i mapB f))
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
        RU.callback(IO(t.blur()), e.preventDefaultIO >> e.stopPropagationIO)
      case _ =>
        nopRU
    }

  val textInputEditor = textEditor(input)
  val textareaEditor  = textEditor(textarea)

  type EditorE[E, A, B, C, D, V] = E => Editor[A, B, C, D, V]

  def renderWithError[A, B, C, D](editor: Editor[A, B, C, D, Modifier])(err: String): Editor[A, B, C, D, Modifier] =
    Editor(ei => div(editor render ei, div(cls := "errorMsg", err)))

  def editorWithError[A, B, C, D](editor: Editor[A, B, C, D, Modifier]): EditorE[Option[String], A, B, C, D, Modifier] =
    _.fold(editor)(renderWithError(editor))

  def editorV[E, A, B, C, D, V](f: A => E, e: EditorE[E, A, B, C, D, V]): Editor[A, B, C, D, V] =
    Editor(i => e(f(i.data)) render i)

  def validateAndDisplayError[A, B, C, D](f: A => Option[String], e: Editor[A, B, C, D, Modifier]): Editor[A, B, C, D, Modifier] =
    Editor(i => editorV(f, editorWithError(e)) render i)

  def composeEditorValidator[I, C, D](v: ValidatorPlus[I, _, _], e: Editor[I, I, C, D, Modifier]): Editor[I, I, C, D, Modifier] =
    applyInputValidation(v,
      applyLiveCorrection(v,
        e))

  def applyLiveCorrection[A, B, C, D, V](v: ValidatorPlus[B, _, _], e: Editor[A, B, C, D, V]): Editor[A, B, C, D, V] =
    e.mapB(v.liveCorrect)

  def applyInputValidation[A, B, C, D](v: ValidatorPlus[A, _, _], e: Editor[A, B, C, D, Modifier]): Editor[A, B, C, D, Modifier] =
    validateAndDisplayError(i => v.correctAndValidate(i).swap.toOption.map(_.toText), e)

  // ===================================================================================================================

  object Example {

    @deprecated("????", "")
    def ???? = scala.Predef.???

    case class Age(value: Int)
    case class Person(id: Long, name: String, age: Age)

    val nameV: ValidatorPlus[String, String, String] = ???

    val ageV =
      ValidatorPlus[String, Option[Int], Age](
        CorrectionPart[String, Option[Int]](s => Try(Option(s.toInt)).getOrElse(None))(_.fold("")(_.toString)),
        ValidationPart[Option[Int], Age](???),
        _.replaceAll("\\D", ""))

    val bothV = nameV *** ageV

    val nameE = textInputEditor
    val ageE = textInputEditor

    val nameE2 = composeEditorValidator(nameV, nameE)
    val ageE2 = composeEditorValidator(ageV, ageE)

    object ManualExample1_split_editors {
      object RowStatus
      case class Props(ppl: Map[Long, Person])
      case class RowState(i: (String,String), rowStatus: RowStatus.type)
      type SavedState = Map[Long, RowState]
      case class ZeState(saved: SavedState)
      val ZS = ReactS.FixT[IO, ZeState]

      class TopBackend(c: BackendScope[Props, ZeState]) {

        def tableProps = TableProps(rowpropsa(c.state.saved))

        def update1(id: Long): (String, RU) => IO[Unit] = ????
        def update2(id: Long): (String, RU) => IO[Unit] =
          (i, ru) => c.runState(
            ru.zoomU[ZeState] >>
              ZS.modS{ s =>
                val nv = s.saved(id).i put2 i
                s.copy(saved = s.saved + (id -> RowState(nv, RowStatus)))
              }
          )

        def revert1(id: Long): RU => IO[Unit] = ????
        def revert2(id: Long): RU => IO[Unit] =
          ru => c.runState(
            ru.zoomU[ZeState] >>
              ZS.modS{ s =>
                // save as update except for this line here ↙
                val i = c.props.ppl(id).age.toString
                val nv = s.saved(id).i put2 i
                s.copy(saved = s.saved + (id -> RowState(nv, RowStatus)))
              }
            )

        def rowpropsa(saved: SavedState): Vector[SavedRowProps] =
          saved.foldLeft(Vector.empty[SavedRowProps])((q,a) => q :+ rowprops1(a._1, a._2))

        def rowprops1(id: Long, s: RowState): SavedRowProps =
          SavedRowProps(id,
            EditorInput(s.i._1, "", Some(EditorCallbacks[String, RU, IO[Unit]](update1(id), revert1(id), ???))),
            EditorInput(s.i._2, "", Some(EditorCallbacks[String, RU, IO[Unit]](update2(id), revert2(id), ???))))
      }

      val outmost = ReactComponentB[Props]("Outmost")
        .getInitialState(p => ZeState(p.ppl.mapValues(v => RowState((v.name, v.age.toString), RowStatus))))
        .backend(new TopBackend(_))
        .render((p, s, b) =>
        div(h1("Hi!"), tablec(b.tableProps))
        )
        .build

      case class TableProps(saved: Vector[SavedRowProps])
      val tablec = ReactComponentB[TableProps]("table")
        .stateless
        .render((p,_) =>
        table(
          thead("Name", "Age"),
          tbody(p.saved.map(savedrow(_)).asJsArray))
        )
        .build

      case class SavedRowProps(key: Long, nameEI: EditorInput[String, String, RU, IO[Unit]], ageEI: EditorInput[String, String, RU, IO[Unit]])
      val savedrow = ReactComponentB[SavedRowProps]("savedrow")
        .stateless
        .render((p, _) => {
        //val (n, a) = e2.render(???)
        val n = nameE2 render p.nameEI
        val a = ageE2 render p.ageEI
        tr(key := p.key, n, a)
      })
        .build
    }
  }
  //  type S = Int
  //  type T = List[Int]
  //  val e1 = textEditor(input)
  //  val e2 = e1.mapC(_.zoomU[S])
  //
  //  val F: ComponentStateFocus[T] = ???
  //  val e3 = e2.mapC(_.zoom2[T](_.head, (a,b) => b :: a))
  //  type ST = ReactST[IO, T, Unit]
  //  val cbs = EditorCallbacks[String, ST, IO[Unit]](
  //    (i,st) => F.runState(st >> updateState(i)),
  //    st => F.runState(st),
  //    st => F.runState(st >> validateSaveLockRow))
  //
  //  def updateState(i: String): ST = ???
  //  def validateSaveLockRow: ST = ???
}
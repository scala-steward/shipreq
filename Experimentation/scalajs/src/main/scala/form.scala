
import japgolly.scalajs.react.vdom.{ReactOutput, ReactVDom, VDomBuilder, ReactFragT}
import org.scalajs.dom
import org.scalajs.dom.extensions.KeyCode
import scala.scalajs.js

import japgolly.scalajs.react._
import vdom.ReactVDom._
import all._
import ScalazReact._

import scalaz.effect.IO
import scalaz.Scalaz.Id
import scalaz.{Foldable, Bind, \/, \/-, -\/}
import scalaz.syntax.bind._
import scalaz.syntax.foldable._

//import golly.ScalazReact._
import monocle._
import monocle.syntax._
import monocle.function.Field1._
import monocle.function.Field2._

import Lib._

/**
 * Done
 * ~~~~
 * escape to cancel change
 * validation as you type
 * input correction (valid or not)
 * saves only when entire row is valid
 *
 * TODO
 * ~~~~
 * [ priority . effort ]
 * [5.5] drag to reorder
 * [5.5] create new
 * [5.3] delete
 * [5.2] show/hide deleted
 * [3.5] different view when field not in edit (sometimes the edit view is too noisy)
 * [3.5] field validity depending on other fields
 * [2.3] visual indication of save-in-progress & save-complete
 * [2.2] server-side only errors / errors on save
 * [1.3] validators with composite types (like new & change password)
 *
 */
object FormStuff {
  private def getOrElseAP[M[_]: Foldable, A](m: M[A], a: => A): A =
    m.foldr(a)(aa => _ => aa)

  private def getOrElseAP[M[_]: Foldable, A, B](m: M[A], b: => B, f: A => B): B =
    m.foldr(b)(a => _ => f(a))

  private def foldableToOption[M[_]: Foldable, A](m: M[A]): Option[A] =
    getOrElseAP[M, A, Option[A]](m, None, Some(_))


  type ErrorMsg = String

  trait Validator[I, C, O] {
    def correct: I => C
    def validate: C => ErrorMsg \/ O
    final def correctAndValidate = validate compose correct
    def c2i: C => I
  }

  trait Editor[D, V] {
    def apply(data: D
              , error: Option[ErrorMsg]
              , onChange: D => IO[Unit]
              , onCancel: IO[Unit] => IO[Unit]
              , onEditEnd: IO[Unit]
               ): V
  }

  // TODO create event handling monad?

  class FormAttrShit[S, I, C, O, M[_] : Bind : Foldable](
                                  v: Validator[I, C, O]
                                  , s2oc: S => Option[C]
                                  , getI: S => M[I]
                                  , putI: (S,I) => M[S]
                                  , trySave: S => IO[S]
                                  ) {

    private def modI(s: S, f: I => I) = getI(s).flatMap(i => putI(s, f(i)))

    private def change(i: I) = (s: S) => getOrElseAP(putI(s,i), s)

    private def cancelChange(T: ComponentScope_SS[S])(c: IO[Unit]): IO[Unit] =
      T.stateIO.flatMap(s =>
        s2oc(s).map(v.c2i) match {
          case None => IO(())
          case Some(i) => T.modStateIO(change(i), c)
        }
      )

    private def editEnd(T: ComponentScope_SS[S]): IO[Unit] =
      T.stateIO.flatMap(s => {
        // optimisation: compare I & I here, don't try to save if equal
        val m = modI(s, v.c2i compose v.correct).map(trySave(_) >>= T.setStateIO)
        getOrElseAP(m, IO(()))
      })

    def render[V](editor: Editor[I, V], T: ComponentScope_SS[S]): M[V] =
      getI(T.state).map(i => {
          val e = v.correctAndValidate(i).swap.toOption
          editor(i, e, i => T modStateIO change(i), cancelChange(T), editEnd(T))
      })
  }

  // ===================================================================================================================
  // Spec

  case class SpecSplice[P, I, C, O](p2c: P => C, v: Validator[I, C, O]) {
    def initial: P => I = v.c2i compose p2c
    def savable(i: I) = v.correctAndValidate(i).toOption
    def edit[V](e: Editor[I, V]) = SpecSpliceE(this, e)
  }

  case class SpecSpliceE[P, V, I, C, O](s: SpecSplice[P, I, C, O], editor: Editor[I, V])

  /**
   * @tparam G "Good", meaning entire row has passed validation, row ready to be saved.
   * @tparam P "Persisted", the last saved copy of the row.
   * @tparam Px "Persisted" with extra info
   * @tparam V "View", the type of the DOM representation.
   *
   * Input
   * - field specs (validation, editor, P → Cₙ)
   * - build: Oₙ               → G
   * - save:  Option[P, Px], G → IO[P]
   *
   * Output
   * - initial: P                          → E
   * - render:  S ↔ P, S ↔ E, Component[S] → Vₙ
   */
  case class Spec2[G, P, Px, V, I1, C1, O1, I2, C2, O2](s1: SpecSpliceE[P,V,I1,C1,O1], s2: SpecSpliceE[P,V,I2,C2,O2]
                                                    , oo2g: ((O1, O2)) => G
                                                    , saveIO: (Option[Px], G) => IO[Px]
                                                    , needSave: (Px, G) => Boolean
                                                     ) {
    type E = (I1,I2)
    type OO = (O1, O2)
    type VV = (V, V)

    def initial(p: P): E = (s1.s initial p, s2.s initial p)

    def savable(e: E): Option[OO] = for {
      o1 <- s1.s.savable(e._1)
      o2 <- s2.s.savable(e._2)
    } yield (o1,o2)

    def shit[S, M[_] : Bind : Foldable](
                                     s2op: S => Option[P],
                                     spp: (S, OO) => IO[S],
                                     getE: S => M[E],
                                     putE: (S,E) => M[S]) = {
      val sf: S => IO[S] = s =>
        foldableToOption(getE(s)).flatMap(savable).fold(IO(s))(oo => spp(s, oo))
      def i1L = _1[E, I1]
      def i2L = _2[E, I2]
      def getI[I](l: SimpleLens[E, I]) = (s: S) => getE(s).map(l.get)
      def putI[I](l: SimpleLens[E, I]) = (s: S, i:I) => getE(s).flatMap(e => putE(s, l.set(e, i)))
      (
      new FormAttrShit[S, I1, C1, O1, M](s1.s.v, s2op.andThen(_ map s1.s.p2c), getI(i1L), putI(i1L), sf),
      new FormAttrShit[S, I2, C2, O2, M](s2.s.v, s2op.andThen(_ map s2.s.p2c), getI(i2L), putI(i2L), sf)
      )
    }

    def render[S](
                    getE: S => E,
                    putE: (S, E) => S,
                    getPx: S => Option[Px],
                    storePx: (S, Option[Px], Px) => S,
                    s2op: S => Option[P]
                    )(T: ComponentScope_SS[S]): VV = renderM[S, Id](getE, putE, getPx, storePx, s2op)(T)

    def renderM[S, M[_] : Bind : Foldable](
                   getE: S => M[E],
                   putE: (S,E) => M[S],
                   getPx: S => Option[Px],
                   storePx: (S, Option[Px], Px) => S,
                   s2op: S => Option[P]
                   )(T: ComponentScope_SS[S]): M[VV] = {


      def spp(s: S, oo: OO): IO[S] = {
        val g = oo2g(oo)
        val prevSave = getPx(s)
        if (prevSave.fold(true)(needSave(_,g))) {
          val iop = saveIO(prevSave, g)
          iop.map(storePx(s, prevSave, _))
        } else IO(s)
      }

      //    def render[S](sp: Lens[S, S, P, OO], se: SimpleLens[S, E])(T: ComponentScope_SS[S]): VV = {
      val s = shit(s2op, spp, getE, putE)
      for {
        v1 <- s._1.render(s1.editor, T)
        v2 <- s._2.render(s2.editor, T)
      } yield (v1,v2)
    }
  }

  // ===================================================================================================================
  // Impl

  object KeyValidator extends Validator[String, String, String] {
    override def correct = _.trim.toUpperCase()
    override def validate = {
      case "" => -\/("It's blank!")
      case s if !s.matches("^[A-Z]+$") => -\/("One word, A-Z only.")
      case s => \/-(s)
    }
    override def c2i = identity
  }

  object DescValidator extends Validator[String, Option[String], Option[String]] {
    override def c2i = _ getOrElse ""
    override def correct = s => {
      val j = s.trim
      if (j.isEmpty) None else Some(j)
    }
    override def validate = \/-(_)
  }

  class TextEditor(node: ReactVDom.Tag) extends Editor[String, ReactVDom.Modifier] {
    override def apply(data: String
                       , error: Option[ErrorMsg]
                       , onChange: String => IO[Unit]
                       , onCancel: IO[Unit] => IO[Unit]
                       , onEditEnd: IO[Unit]
                        ) = {

      val cancelOnEscape: InputEvent => IO[Unit] =
        e => e.keyboardEvent
          .filter(_.keyCode == KeyCode.escape)
          .fold(IO(()))(_ => {
          val t = e.target
          e.preventDefaultIO >> e.stopPropagationIO >> onCancel(IO(t.blur()))
        })

      div(
        node(
          value := data
          , error.isDefined && (cls := "error")
          , onchange  ~~> textChangeRecvIO(onChange)
          , onblur    ~~> onEditEnd
          , onkeydown ~~> cancelOnEscape
        )
        , error.fold(Nop)(e => div(cls := "errorMsg")(e))
      )
    }
  }

  val TextInputEditor = new TextEditor(input)
  val TextareaEditor = new TextEditor(textarea)
}

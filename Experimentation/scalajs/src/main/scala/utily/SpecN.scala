package utily

import japgolly.scalajs.react._
import scalaz.effect.IO
import scalaz.{Foldable, Bind}
import scalaz.syntax.bind._
import monocle.function.Field1._
import monocle.function.Field2._
import FormStuff._
import EditorStuff._

object SpecN {

  /**
   * Single field attribute: types & validation logic.
   */
  case class SpecSplice[P, I, C, O](p2c: P => C, v: Validator[I, C, O]) {
    def initial: P => I = v.c2i compose p2c
    //    def savable(i: I) = v.correctAndValidate(i).toOption
    def edit[V](e: Editor[I, V]) = SpecSpliceE(this, e)
  }

  /**
   * Single field attribute: +renderToView.
   */
  case class SpecSpliceE[P, V, I, C, O](s: SpecSplice[P, I, C, O], editor: Editor[I, V])

  /**
   * This is actually just field attribute composition.
   * Single row/record.
   *
   * @tparam G "Good", meaning entire row has passed validation, row ready to be saved.
   * @tparam P "Persisted", the last saved copy of the row.
   * @tparam V "View", the type of the DOM representation.
   */
  case class Spec2[G, P, V, I1, C1, O1, I2, C2, O2](s1: SpecSpliceE[P,V,I1,C1,O1], s2: SpecSpliceE[P,V,I2,C2,O2]
                                                    , oo2g: ((O1, O2)) => G
                                                     ) {
    type E = (I1,I2)
    type OO = (O1, O2)
    type VV = (V, V)

    def initial(p: P): E = (s1.s initial p, s2.s initial p)

    def forState[S] =
      Spec2X[S, Unit, G, P, V, I1, C1, O1, I2, C2, O2](this, None, None).forRow(())
  }

  /**
   * Field attributes + TABLE ROW-ID + [ROW-ID & STATE AWARE VALIDATORS]
   */
  case class Spec2X[S, W, G, P, V, I1, C1, O1, I2, C2, O2](
      spec: Spec2[G, P, V, I1, C1, O1, I2, C2, O2],
      ctxV1: Option[CtxValidation[S, W, O1]],
      ctxV2: Option[CtxValidation[S, W, O2]]
      ) {
    type E = spec.E
    type VV = spec.VV

    import spec.{OO, s1, s2, oo2g}

    private def fieldRenderers[M[_] : Bind : Foldable](s2mp: S => M[P],
                                                       w: W,
                                                       saveG: (S, G) => IO[S],
                                                       eL: WierdLens[M, S, S, E]) = {

      val v1 = ctxV1.fold[S => Validator[I1,C1,O1]]( _ => s1.s.v )( c=> ValidatorX2(s1.s.v, c, w) )
      val v2 = ctxV2.fold[S => Validator[I2,C2,O2]]( _ => s2.s.v )( c=> ValidatorX2(s2.s.v, c, w) )

      //def savable1(i: I) = v.correctAndValidate(i).toOption
      def savable(s: S, e: E): Option[OO] = for {
        o1 <- v1(s).correctAndValidate(e._1).toOption
        o2 <- v2(s).correctAndValidate(e._2).toOption
      } yield (o1,o2)

      // TODO This isn't able to traverse other rows, or access them by W
      // Cannot save rows that were invalid due to this one, that have just become valid.
      val sf: S => IO[S] = s =>
        foldableToOption(eL.get(s)).flatMap(savable(s,_)).fold(IO(s))(oo => saveG(s, oo2g(oo)))

      (
        new FormAttrShit[S, I1, C1, O1, M](v1, s2mp.andThen(_ map s1.s.p2c), eL map _1[E, I1], sf),
        new FormAttrShit[S, I2, C2, O2, M](v2, s2mp.andThen(_ map s2.s.p2c), eL map _2[E, I2], sf)
        )
    }

    def forRow(w: W): Renderable[S, G, P, E, V, VV] = new Renderable[S, G, P, E, V, VV] {
      override def renderM[M[_] : Bind : Foldable]
      (eL: WierdLens[M, S, S, E], s2mp: S => M[P])
      (saveG: (S, G) => IO[S]): ComponentStateFocus[S] => M[VV] = T => {
        val s = fieldRenderers(s2mp, w, saveG, eL)
        for {
          v1 <- s._1.render(s1.editor, T)
          v2 <- s._2.render(s2.editor, T)
        } yield (v1,v2)
      }
    }
  }

  def SpecBuilder[P] = new {
    def apply[V, I1, C1, O1, I2, C2, O2](s1: SpecSpliceE[P,V,I1,C1,O1], s2: SpecSpliceE[P,V,I2,C2,O2]) =
      new SpecBuilder2[P, (O1,O2), V, I1, C1, O1, I2, C2, O2](s1, s2, oo=>oo)
  }

  class SpecBuilder2[P, O, V, I1, C1, O1, I2, C2, O2](
      s1: SpecSpliceE[P,V,I1,C1,O1], s2: SpecSpliceE[P,V,I2,C2,O2],
      buildO: ((O1,O2)) => O) {

    type I = (I1,I2)
    type VV = (V,V)

    def mapO[OO](f: O => OO) = new SpecBuilder2(s1,s2, f compose buildO)
    def buildO[OO](f: (O1,O2) => OO) = new SpecBuilder2(s1,s2, f.tupled)

    def rowId[W] = new B2[W]

    class B2[DataId] {
      type RowId = Option[DataId]
      private type Unsaved = Option[I]
      private type Saved = Map[DataId, (P, I)]
      type S = (Saved, Unsaved)
      private val savedL = _1[S, Saved]
      private val unsavedL = _2[S, Unsaved]
      type Px = (DataId, P)
      type OO = O

      def uniquenessCheck[A](f: P => A) = uniqueness[S, RowId, (DataId, (P, I)), A](
        (s,ow) => savedL.get(s).toStream.filterNot(wpi => ow.fold(false)(_ == wpi._1)),
        (wpi,a) => a == f(wpi._2._1)
      )

      def ctxAwareValidators(cv1: Option[CtxValidation[S, RowId, O1]], cv2: Option[CtxValidation[S, RowId, O2]]) = new {
        def saveFn(saveIO: (Option[Px], O) => IO[Px]) = {
          val spec = Spec2X(Spec2(s1, s2, buildO), cv1, cv2)
          val renderable: Option[DataId] => Renderable[S,O,P,I,V,VV] = spec.forRow
          val PtoI: P => I = spec.spec.initial
          def mkPI(p: P): (P,I) = (p, PtoI(p))
          val initialState: Seq[(DataId, P)] => S = xs => (xs.map(x => x._1 -> mkPI(x._2)).toMap, None)
          new TableSpec(renderable, savedL, unsavedL, PtoI, initialState, saveIO)
        }
      }
    }
  }
}

package exp

import monocle._
import monocle.macros.Lenses
import org.scalajs.dom, dom.ext.KeyCode
import shipreq.base.util.Util
import scalajs.js
import scalaz.Equal
import scalaz.std.anyVal.intInstance
import scalaz.std.string.stringInstance
import scalaz.syntax.equal._
import japgolly.scalajs.react._, vdom.prefix_<^._, MonocleReact._
import japgolly.scalajs.react.extra._

/**
  * Preview available:
  * - when editing and focused and (dirty or has been edited since receiving focus)
  *
  * Editor opens:
  * - when clicked
  * - when navigated to by KB
  *
  * Editor closes:
  * - on commit (enter)
  * - on abort (escape)
  * - when loses focus and there is no change
  */
object FocusPreviewExperiment {

  import CompState._

  @inline implicit def MonocleReactCompStateOpsDD2[$, S]($: $)(implicit ops: $ => ReadDirectWriteDirectOps[S]) =
    new MonocleReactCompStateOps2[ReadDirectWriteDirectOps[S], S, Unit](ops($))

  @inline implicit def MonocleReactCompStateOpsDC2[$, S]($: $)(implicit ops: $ => ReadDirectWriteCallbackOps[S]) =
    new MonocleReactCompStateOps2[ReadDirectWriteCallbackOps[S], S, Callback](ops($))

  @inline implicit def MonocleReactCompStateOpsCC2[$, S]($: $)(implicit ops: $ => ReadCallbackWriteCallbackOps[S]) =
    new MonocleReactCompStateOps2[ReadCallbackWriteCallbackOps[S], S, Callback](ops($))

  final class MonocleReactCompStateOps2[Ops <: WriteOpAux[S, W], S, W](private val $: Ops) extends AnyVal {
    def setStateL[L[_, _, _, _], B](l: L[S, S, _, B])(b: B, cb: Callback = Callback.empty)(implicit L: SetterMonocle[L]): W =
      $.modState(L.set(l)(b), cb)
  }

  def main(): Unit = {
    val tgt = dom.document.getElementById("target")
    ReactDOM.render(Table.Comp(), tgt)
  }

  // ===================================================================================================================

  object PreviewLogic {

    case class FocusData[+K](key: K, changedSinceFocus: Boolean)

    class PreviewStuff[S, E, K]($: BackendScope[_, S],
                         focusLens: Lens[S, Option[FocusData[K]]]
                         //,isDirty: (S, E) => Boolean
                         )
                        (implicit EK  : Equal[K]){

      private val hasKey: K => FocusData[K] => Boolean =
        if (EK.equalIsNatural)
          k => _.key == k
        else
          k => fi => EK.equal(fi.key, k)

      def onFocus(k: K): Callback =
        $.modState(s =>
          if (focusLens.get(s) exists hasKey(k))
            s
          else
            focusLens.set(Some(FocusData(k, false)))(s))

      def onBlur(k: K): Callback =
        $.modState(s =>
          if (focusLens.get(s) exists hasKey(k))
            focusLens.set(None)(s)
          else
            s)

      def onEdit(k: K): Callback =
        $.modState(s =>
          if (!focusLens.get(s).exists(i => i.changedSinceFocus && hasKey(k)(i)))
            focusLens.set(Some(FocusData(k, true)))(s)
          else
            s)

      def showPreview(focusData: Option[FocusData[K]], isDirty: => Boolean): Boolean =
        focusData.exists(_.changedSinceFocus || isDirty)

      def forChild(k: K, fi: Option[FocusData[K]]): PreviewForChild[K] =
        new PreviewForChild[K] {
          override val focusData: Option[FocusData[K]] =
            fi.filter(hasKey(k))
          override def editorMods[A](a0: A)(blur: (A, Callback) => A, focus: (A, Callback) => A, edit: (A, Callback) => A): A = {
            var a = a0
            a = blur(a, onBlur(k))
            a = focus(a, onFocus(k))
            a = edit(a, onEdit(k))
            a
          }
          override def showPreview(isDirty: => Boolean): Boolean =
            PreviewStuff.this.showPreview(focusData, isDirty)
        }
    }

    trait PreviewForChild[+K] {
      val focusData: Option[FocusData[K]]
      def editorMods[A](a: A)(blur: (A, Callback) => A, focus: (A, Callback) => A, edit: (A, Callback) => A): A
      def showPreview(isDirty: => Boolean): Boolean
    }

  } // PreviewLogic

  /*
  object Lib {

    case class FocusInfo[+K](key: K, changedSinceFocus: Boolean)

    case class DataThingy[V, E, +K](value: V, edit: ExternalVar[Option[E]], focusInfo: Option[FocusInfo[K]],
                                    focusSelf: Callback, onFocus: Callback, onBlur: Callback) {

      def abort: Callback =
        edit set None

      def editorMod: TagMod =
        TagMod(
          ^.onFocus --> onFocus,
          ^.onBlur --> onBlur)

      def showPreviewE(implicit ev: E =:= V, equal: Equal[V]): Boolean =
        showPreview(equal.equal(_, _))

      def showPreview(equal: (V, E) => Boolean): Boolean =
        focusInfo.exists { fi =>
          def isDirty = edit.value.exists(e => !equal(value, e))
          fi.changedSinceFocus || isDirty
        }

      def focusOnClick: TagMod =
        edit.value match {
          case None    => TagMod(^.onClick --> focusSelf)
          case Some(_) => EmptyTag
        }

    }

    implicit class DataThingyES[V, K](private val d: DataThingy[V, String, K]) extends AnyVal {
      def inputText(e: String): ReactTagOf[dom.html.Input] =
        <.input(
          ^.`type` := "text",
          d.editorMod,
          ^.onChange ==> ((e: ReactEventI) => d.edit set Some(e.target.value)),
          ^.value := e)
    }

    class Methods[S, K: Equal, V, E]($: BackendScope[_, S])
                                    (focusLens: Lens[S, Option[FocusInfo[K]]])
                                    (editLens: K => Lens[S, Option[E]])
                                    (getValue: (S, K) => V)
                                    (isEditUseless: (V, E) => Boolean,
                                     initEdit: V => E,
                                     tryToFocus: K => Callback) {
      private val EK = Equal[K]

      private val hasKey: K => FocusInfo[K] => Boolean =
        if (EK.equalIsNatural)
          k => _.key == k
        else
          k => fi => EK.equal(fi.key, k)

      def onFocus(k: K): Callback =
        $.modState(s =>
         if (focusLens.get(s) exists hasKey(k))
           s
         else
           focusLens.set(Some(FocusInfo(k, false)))(s))

      def onBlur(k: K): Callback =
        $.modState {
        val el = editLens(k)
        s0 => {
          var s = s0
          if (focusLens.get(s) exists hasKey(k))
            s = focusLens.set(None)(s)

          for (e <- el.get(s))
            if (isEditUseless(getValue(s, k), e))
              s = el.set(None)(s)

          s
        }
      }

     def onEdit(k: K): Option[E] => Callback =
       no => $.modState{s0 =>
         val lens = editLens(k)
         var s = lens.set(no)(s0)
         no match {
           case None => // remove focus? No, editor will be removed when state cleared
           case Some(n) =>
             // TODO make efficient
             s = focusLens.set(Some(FocusInfo(k, true)))(s)
         }
         s
       }

      def editVar(s: S, k: K): ExternalVar[Option[E]] =
        ExternalVar(editLens(k) get s)(onEdit(k))

      def startEditor(k: K): Callback =
        $.modState(
          s => editLens(k).modify(_ orElse Some(initEdit(getValue(s, k))))(s),
          tryToFocus(k))

      def focus(k: K)(s: S): Callback = {
        editLens(k).get(s) match {
          case None    => startEditor(k)
          case Some(_) => tryToFocus(k)
        }
      }

      def dataThingy(s: S, k: K): DataThingy[V, E, K] =
        DataThingy[V, E, K](
          getValue(s, k),
          editVar(s, k),
          focusLens get s filter hasKey(k),
          Callback byName focus(k)(s),
          onFocus(k),
          onBlur(k))
    }

  }
  */

  // ===================================================================================================================
  import PreviewLogic._


  object Table {
    val sampleData = Vector[String](
      "blah [blah] #1",
      "blah",
      "blah blah",
      "[blah] blah [blah]")

    val Id = "FocusPreviewExperiment"

    @Lenses
    case class State(values: Vector[String], editorStates: Map[Int, String], focus: Option[FocusData[Int]])

    object State {
      import monocle._, Monocle._

      def forValue(i: Int): Optional[State, String] =
        State.values ^|-? index(i)

      def forRow(i: Int): Lens[State, Option[String]] =
        State.editorStates ^|-> at(i)
    }

    class Backend($: BackendScope[Unit, State]) {
//      val FM = new Methods[State, Int, String, String]($)(State.focus)(State.forRow)(_ values _)(_ == _, identity, tryFocus)

      val FM = new PreviewStuff[State, String, Int]($, State.focus)


      def ref(i: Int) = Ref.to(Row.Comp, "row_" + i)

      def getRowComp(i: Int) =
        CallbackTo(ref(i)($).get)

      def getRowEditor(i: Int): CallbackTo[js.UndefOr[dom.html.Input]] =
        getRowComp(i) map (c => c.backend.ref(c))

      def tryToFocus(i: Int): Callback =
        getRowEditor(i).flatMap(_.tryFocus)

      def moveFocus(s: State, i: Int): Callback = {
        val newIndex = Util.fitCollectionIndex(i, s.values.length)
        focus(newIndex)(s)
      }


      type K = Int
      type S = State
      val editLens = State.forRow _
      def getValue(s: S, k: K) = s.values(k)
      val initEdit = identity[String] _
      def startEditor(k: K): Callback =
        $.modState(
          s => editLens(k).modify(_ orElse Some(initEdit(getValue(s, k))))(s),
          tryToFocus(k))

      def focus(k: K)(s: S): Callback = {
        editLens(k).get(s) match {
          case None    => startEditor(k)
          case Some(_) => tryToFocus(k)
        }
      }


      def render(s: State) =
        <.table(
          ^.id := Id,
          <.tbody(
            s.values.zipWithIndex.map { case (v, i) =>

//              val dt = FM.dataThingy(s, i)
              val fc = FM.forChild(i, s.focus)

              val lens = State.forRow(i)
              val focusUp  : Callback = moveFocus(s, i-1)
              val focusDown: Callback = moveFocus(s, i+1)
              val commit: String => Callback = n => $.modState(lens.set(None) compose State.forValue(i).set(n))

//              val edit = ExternalVar.state($ zoomL State.forRow(i)) // TODO
              val edit = ExternalVar(State.forRow(i) get s)(e => $ modState State.forRow(i).set(e))

              val rp = Row.Props(v, edit, fc, startEditor(i), commit, focusUp, focusDown)

              <.tr(
                ^.key := i,
                Row.Comp.withRef(ref(i))(rp))
            }))
    }

    val Comp = ReactComponentB[Unit]("Outer")
      .initialState(State(sampleData, Map.empty, None))
      .renderBackend[Backend]
      .buildU
  }

  object Row {

    case class Props(value: String,
                     edit: ExternalVar[Option[String]],
                      preview: PreviewForChild[Any],
                     startEditor: Callback,
                      commit: String => Callback, focusUp: Callback, focusDown: Callback)

    object SimpleParser {
      val token = """^(.*?)\[([^\[]+?)\](.*)$""".r

      def append(q: Vector[ReactTag], s: String) =
        if (s.isEmpty) q else q :+ <.span(s)

      @scala.annotation.tailrec
      def go(s: String, acc: Vector[ReactTag]): Vector[ReactTag] = {
        val m = token.pattern.matcher(s)
        if (m.matches) {
          var q = append(acc, m group 1)
          q :+= <.span(^.color := "red", ^.backgroundColor := "#ddd", ^.padding := "0 6px", m group 2)
          go(m group 3, q)
        }
        else
          append(acc, s)
      }

      def apply(s: String): ReactTag =
        <.span(go(s, Vector.empty): _*)
    }

    // Values here correspond to values in CSS in index.html
//    val tg = Addons.ReactCssTransitionGroup("fadeanim", enterTimeout = 110, leaveTimeout = 110, component = "div")

    class Backend($: BackendScope[Props, Unit]) {

      val ref = Ref[dom.html.Input]("i")

      def render(p: Props): ReactElement = {

        val inner = p.edit.value match {
          case None =>
            SimpleParser(p.value)

          case Some(es) =>
            def onKey(e: ReactKeyboardEventI): Callback =
              CallbackOption.keyCodeSwitch(e) {
                case KeyCode.Escape => p.edit set None
                case KeyCode.Enter => p commit es
                case KeyCode.Down => p.focusDown
                case KeyCode.Up => p.focusUp
              }

            val tagMod = p.preview.editorMods(EmptyTag)(
              blur  = (t, cb) => t + (^.onBlur   --> (
                cb >> Callback.ifTrue(es == p.value, p.edit.set(None))
                )),
              focus = (t, cb) => t + (^.onFocus  --> cb),
              edit  = (t, cb) => t + (^.onChange ==> ((e: ReactEventI) =>
                cb >> p.edit.set(Some(e.target.value))
                )))

            val input =
              <.input(
                ^.`type` := "text",
                tagMod,
                ^.value := es,
                ^.backgroundColor := (if (p.preview.focusData.isDefined) "#ffc" else "#f2f2d6"),
                ^.ref := ref,
                ^.onKeyDown ==> onKey)

            def preview =
              ReactCollapse(p.preview.showPreview(es != p.value))(
                <.div(^.key := 9,
                  <.div("Preview:"),
                  <.div(^.backgroundColor := "#efe", SimpleParser(es))))

            <.div(input, preview)
        }

        <.td(
          ^.border := "solid 1px #444",
          ^.padding := "0.5ex 1ex",
          ^.width := "30ex",
          ^.onClick --> p.startEditor,
//          p.focusOnClick,
          inner)
      }
    }

    val Comp = ReactComponentB[Props]("Row")
      .renderBackend[Backend]
      .build
  }
}

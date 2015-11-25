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
/*
  object Lib {

    @Lenses
    case class FocusInfo[+K](key: K, changedSinceFocus: Boolean)

    @Lenses
    case class FocusCmds(focusUp: Callback, focusDown: Callback,
                         focusSelf: Callback,
                         onFocus: Callback, onBlur: Callback)

    case class DataThingy[A, S](value: A, edit: ExternalVar[Option[S]], focusInfo: Option[FocusInfo], focusCmds: FocusCmds) {

      def abort: Callback =
        edit set None

      def showPreviewE(implicit ev: S =:= A, equal: Equal[A]): Boolean =
        showPreview(equal.equal(_, _))

      def showPreview(equal: (A, S) => Boolean): Boolean =
        focusInfo.exists { fi =>
          def isDirty = edit.value.exists(e => !equal(value, e))
          fi.changedSinceFocus || isDirty
        }
    }


    def doBlur[S, K, V, E](focusLens: Lens[S, Option[FocusInfo[K]]])
                          (getValue: S => V,
                           editLens: K => Lens[S, Option[E]]): K => S => S =
      k => s0 => {
        var s = s0
        if (focusLens.get(s).exists(hasKey(k)))
          s = focusLens.set(None)(s)

        if (es.exists(_ == v))
          s = lens.set(None)(s)

        s
      }
  }
  */

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

  // ===================================================================================================================
  import Lib._


  object Table {
    val sampleData = Vector[String](
      "blah [blah] #1",
      "blah",
      "blah blah",
      "[blah] blah [blah]")

    val Id = "FocusPreviewExperiment"

    @Lenses
    case class State(values: Vector[String], editorStates: Map[Int, String], focus: Option[FocusInfo[Int]])

    object State {
      import monocle._, Monocle._

      def forValue(i: Int): Optional[State, String] =
        State.values ^|-? index(i)

      def forRow(i: Int): Lens[State, Option[String]] =
        State.editorStates ^|-> at(i)
    }

    class Backend($: BackendScope[Unit, State]) {
      val FM = new Methods[State, Int, String, String]($)(State.focus)(State.forRow)(_ values _)(_ == _, identity, tryFocus)

      def ref(i: Int) = Ref.to(Row.Comp, "row_" + i)

      def getRowComp(i: Int) =
        CallbackTo(ref(i)($).get)

      def getRowEditor(i: Int): CallbackTo[js.UndefOr[dom.html.Input]] =
        getRowComp(i) map (c => c.backend.ref(c))

      def tryFocus(i: Int): Callback =
        getRowEditor(i).flatMap(_.tryFocus)

      def moveFocus(s: State, i: Int): Callback = {
        val newIndex = Util.fitCollectionIndex(i, s.values.length)
        FM.focus(newIndex)(s)
      }

      def render(s: State) =
        <.table(
          ^.id := Id,
          <.tbody(
            s.values.zipWithIndex.map { case (v, i) =>

              val dt = FM.dataThingy(s, i)

              val lens = State.forRow(i)
              val focusUp  : Callback = moveFocus(s, i-1)
              val focusDown: Callback = moveFocus(s, i+1)
              val commit: String => Callback = n => $.modState(lens.set(None) compose State.forValue(i).set(n))

              val rp = Row.Props(dt, commit, focusUp, focusDown)

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

    case class Props(d: DataThingy[String, String, Any], commit: String => Callback, focusUp: Callback, focusDown: Callback)

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

      def render(pp: Props): ReactElement = {
        val p = pp.d

        val inner = p.edit.value match {
          case None =>
            SimpleParser(p.value)

          case Some(es) =>
            def onKey(e: ReactKeyboardEventI): Callback =
              CallbackOption.keyCodeSwitch(e) {
                case KeyCode.Escape => p.abort
                case KeyCode.Enter => pp commit es
                case KeyCode.Down => pp.focusDown
                case KeyCode.Up => pp.focusUp
              }

            val input =
              p.inputText(es)(
                ^.backgroundColor := (if (p.focusInfo.isDefined) "#ffc" else "#f2f2d6"),
                ^.ref := ref,
                ^.onKeyDown ==> onKey)

            def preview =
              ReactCollapse(p.showPreviewE)(
                <.div(^.key := 9,
                  <.div("Preview:"),
                  <.div(^.backgroundColor := "#efe", SimpleParser(es))))

            <.div(input, preview)
        }

        <.td(
          ^.border := "solid 1px #444",
          ^.padding := "0.5ex 1ex",
          ^.width := "30ex",
          p.focusOnClick,
          inner)
      }
    }

    val Comp = ReactComponentB[Props]("Row")
      .renderBackend[Backend]
      .build
  }
}

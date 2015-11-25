package exp

import monocle._
import monocle.macros.Lenses
import org.scalajs.dom, dom.ext.KeyCode
import shipreq.base.util.Util
import scalajs.js
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

  @Lenses
  case class FocusInfo(index: Int, changedSinceFocus: Boolean)
//            def onChange(e: ReactEventI): Callback =
//              $.modState(s => s.copy(edit = Some(e.target.value), changedSinceFocus = true))
//            def onFocus: Callback =
//              $.modState(s => s.copy(focus = true, changedSinceFocus = false))
//            def onBlur: Callback =
//              $.modState { s =>
//                val newEdit = s.edit.filter(_ != s.value)
//                s.copy(edit = newEdit, focus = false, changedSinceFocus = false)
//              }

  @Lenses
  case class FocusCmds(focusUp: Callback, focusDown: Callback,
                       focusSelf: Callback, //focusNothing: Callback,
                       onFocus: Callback, onBlur: Callback)

  object Table {
    val sampleData = Vector[String](
      "blah [blah] #1",
      "blah",
      "blah blah",
      "[blah] blah [blah]")

    val Id = "FocusPreviewExperiment"

    @Lenses
    case class State(values: Vector[String], editorStates: Map[Int, String],focus: Option[FocusInfo])

    object State {
      import monocle._, Monocle._

      def forValue(i: Int): Optional[State, String] =
        State.values ^|-? index(i)

      def forRow(i: Int): Lens[State, Option[String]] =
        State.editorStates ^|-> at(i)
    }

    class Backend($: BackendScope[Unit, State]) {

      def ref(i: Int) = Ref.to(Row.Comp, "row_" + i)

      def getRowComp(i: Int) =
        CallbackTo(ref(i)($).get)

      def getRowEditor(i: Int): CallbackTo[js.UndefOr[dom.html.Input]] =
        getRowComp(i) map (c => c.backend.ref(c))

      def startEditor(i: Int): Callback =
        $.modState(
          s => State.forRow(i).modify(_ orElse Some(s.values(i)))(s),
          focusRowEditor(i))

      def focusRowEditor(i: Int): Callback =
        getRowEditor(i).flatMap(_.tryFocus)

      def focusMove(s: State, i: Int): Callback = {
        val newIndex = Util.fitCollectionIndex(i, s.values.length)
        focusI(newIndex)(s)
      }

      def focusI(i: Int)(s: State): Callback = {
        State.forRow(i).get(s) match {
          case None    => startEditor(i)
          case Some(_) => focusRowEditor(i)
        }
      }

      def doFocus(i: Int): Callback =
        $.modState(s =>
          s.focus.filter(_.index == i) match {
            case None => s.copy(focus = Some(FocusInfo(i, false)))
            case Some(fi) => s
          }
        )

      def render(s: State) =
        <.table(
          ^.id := Id,
          <.tbody(
            s.values.zipWithIndex.map { case (v, i) =>

              val lens = State.forRow(i)
              val es = lens get s

              def doEdit(no: Option[String]): Callback =
                $.modState{s0 =>
                  var s = lens.set(no)(s0)
                  no match {
                    case None => // remove focus? No, editor will be removed when state cleared
                    case Some(n) =>
                      // TODO make efficient
                      s = s.copy(focus = Some(FocusInfo(i, true)))
                  }
                  s
                }
              def doBlur: Callback =
                $.modState{s0 =>
                  var s = s0
                  s.focus.filter(_.index == i) match {
                    case None => ()
                    case Some(fi) => s = s.copy(focus = None)
                  }

                  if (es.exists(_ == v))
                    s = lens.set(None)(s)

                  s
                }

              val focusUp     : Callback = focusMove(s, i-1)
              val focusDown   : Callback = focusMove(s, i+1)
              val focusSelf   : Callback = focusI(i)(s)
//            val focusNothing: Callback =
              val onFocus     : Callback = doFocus(i)
              val onBlur      : Callback = doBlur

              val value: String                     = v
              val edit: ExternalVar[Option[String]] = ExternalVar(es)(doEdit)
              val commit: String => Callback        = n => $.modState(lens.set(None) compose State.forValue(i).set(n))
              val focusInfo: Option[FocusInfo]      = s.focus.filter(_.index == i)
              val focusCmds: FocusCmds              = FocusCmds(focusUp,focusDown,focusSelf,onFocus,onBlur)

              val rp = Row.Props(value, edit, commit, focusInfo, focusCmds)

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

    // Option[(ExternalVar[String], Option[FocusInfo])]
    case class Props(value: String, edit: ExternalVar[Option[String]], commit: String => Callback, focusInfo: Option[FocusInfo], focusCmds: FocusCmds)

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

//      val startEdit: Callback =
//        $.props >>= (p => p.edit set Some(p.value))
//        $.modState(s => s.copy(edit = Some(s.value)), Callback byName ref($).tryFocus)

      def render(p: Props): ReactElement = {

        val inner = p.edit.value match {
          case None =>
            SimpleParser(p.value)

          case Some(es) =>
            def onChange(e: ReactEventI): Callback =
              p.edit set Some(e.target.value)

            def onKey(e: ReactKeyboardEventI): Callback =
              CallbackOption.keyCodeSwitch(e) {
                case KeyCode.Escape => p.edit set None
                case KeyCode.Enter => p commit es
                case KeyCode.Down => p.focusCmds.focusDown
                case KeyCode.Up => p.focusCmds.focusUp
              }

            val input =
              <.input(
                ^.backgroundColor := (if (p.focusInfo.isDefined) "#ffc" else "#f2f2d6"),
                ^.ref := ref,
                ^.`type` := "text",
                ^.onChange ==> onChange,
                ^.onKeyDown ==> onKey,
                ^.onFocus --> p.focusCmds.onFocus,
                ^.onBlur --> p.focusCmds.onBlur,
                ^.value := es)

            val showPreview =
              p.focusInfo match {
                case None => false
                case Some(i) =>
                  def isDirty = es != p.value
                  i.changedSinceFocus || isDirty
              }

            def preview =
              ReactCollapse(showPreview)(
                <.div(^.key := 9,
                  <.div("Preview:"),
                  <.div(^.backgroundColor := "#efe", SimpleParser(es))))

            <.div(input, preview)
        }

        val outer = p.edit.value match {
          case None =>
            TagMod(
              ^.onClick --> p.focusCmds.focusSelf)

          case Some(_) =>
            TagMod()
        }

        <.td(
          ^.border := "solid 1px #444",
          ^.padding := "0.5ex 1ex",
          ^.width := "30ex",
          outer,
          inner)
      }
    }

    val Comp = ReactComponentB[Props]("Row")
      .renderBackend[Backend]
      .build
  }
}

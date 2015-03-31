package shipreq.webapp.client.app.ui.reqtable

import java.util.regex.Pattern
import japgolly.scalacss.ScalaCssReact._
import japgolly.scalajs.jquery.{TextComplete => TC}
import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import org.scalajs.dom.console
import org.scalajs.dom.ext.KeyValue
import org.scalajs.dom.raw.HTMLInputElement
import shipreq.webapp.client.lib.ui.UI
import scalajs.js

import scalaz.{\/, -\/, \/-, Tags}
import scalaz.effect.IO
import scalaz.std.vector._
import scalaz.std.option._
import scalaz.std.stream._
import scalaz.syntax.foldable._

import shipreq.base.util.ScalaExt._
import shipreq.base.util.{Must, UnivEq, Rx}
import shipreq.base.util.effect.IoUtils, IoUtils.IoExt
import shipreq.webapp.base.{Grammar, UiText}
import shipreq.webapp.client.app.ui.Style

object TextSeqEditor {
  type S = String
  type ParseResult[+O] = Option[String] \/ O

  type AutoComplete = Rx[TC.Strategies]

  final case class Format(normAll: EndoFn[String], sep: Pattern, normEach: EndoFn[String], ignore: String => Boolean) {
    def apply(input: String): Stream[String] =
      (input |> normAll |> sep.split).toStream map normEach filterNot ignore
  }

  val hashtagSeqFormat = {
    val each = "^# *".r
    Format(_.trim, "[# ,]+".r.pattern, each.replaceFirstIn(_, ""), _.isEmpty)
  }

  val leftNone: ParseResult[Nothing] =
    -\/(None)
}

import TextSeqEditor._

final class TextSeqEditor[A](fmt: Format) {

  case class Props(state       : S,
                   stateUpdate : S => IO[Unit],
                   abort       : IO[Unit],
                   parse       : S => ParseResult[A],
                   commit      : Vector[A] => IO[Unit],
                   autoComplete: AutoComplete)

  val component =
    ReactComponentB[Props]("TextSeqEditor")
      .stateless
      .backend(new Backend(_))
      .render(_.backend.render)
      .componentDidMount { $ =>
        val n = $.getDOMNode().asInstanceOf[HTMLInputElement]
        n.focus()
        n.select()

        // TODO Should update autoComplete if needed on props change
        val strategies = $.props.autoComplete.value()
        if (strategies.nonEmpty) {
          val nn = js.Dynamic.global.$(n)
          TC(nn, strategies)
          TC.onSelect(nn) {
            $.props.stateUpdate(n.value).unsafePerformIO()
          }
        }
      }
      .domType[HTMLInputElement]
      .build

  class Backend($: BackendScope[Props, Unit]) {

    val cancelOnEscape = UI.keyDispatch(_.key) {
      case KeyValue.Escape => $.props.abort
    }

    val onChange: ReactEventI => IO[Unit] =
      e => $.props.stateUpdate(e.target.value)

    def render: ReactElement = {
      val p = $.props

      val parseResult =
        fmt(p.state)
          .map(p.parse(_).bimap(Tags.First.apply, Vector.empty :+ _))
          .suml

      def onKeyPress = UI.keyDispatch(_.key) {
        case KeyValue.Enter => parseResult.fold(_ => js.undefined, p.commit)
      }

      <.input(
        Style.reqtable.cellEditor(parseResult.isLeft),
        ^.`type`      := "text",
        ^.value       := p.state,
        ^.onChange   ~~> onChange,
        ^.onKeyDown  ~~> cancelOnEscape,
        ^.onKeyPress ~~> onKeyPress)
    }
  }
}

// =====================================================================================================================

object TagEditor {
  import shipreq.webapp.base.data._

  type A = ApplicableTag.Id

  type Lookup = Map[String, A] // TODO ¿ case class Lookup(legal: Map[String, A], suggest: Set[String]) ?

  final val editor = new TextSeqEditor[A](hashtagSeqFormat)

  def lookupForNoCol(p: Rx[Project]): Rx[Lookup] =
    lookupRx(p, _.tagsNotUsedInColumns)

  def lookupForCol(p: Rx[Project], f: CustomField.Tag.Id): Rx[Lookup] =
    lookupRx(p, _.tagsForColumn(f))

  def lookupRx(project: Rx[Project], f: TagColumnDistribution => Must[Set[ApplicableTag]]): Rx[Lookup] =
    project.map(p =>
      mustResolve(f(p.tagColumnDistribution))(UnivEq.emptySet)
        .toStream
        .map(_.tmap2(_.key.value, _.id))
        .toMap)

  def apply(initial : Vector[A],
            project : Project,
            lookup  : Rx[Lookup],
            setState: Option[Cell.State] => IO[Unit]): CellState = {

    val init: S =
      initial.map { a =>
        val m = project.atag(a).map(_.key.value)
        UiText.mustA(m)
      } mkString " "

    val autoComplete: AutoComplete =
      lookup.map(l =>
        TC.Strategies(
          TC.Strategy(s"\\b(${Grammar.hashRefKeyChars.+})$$")
            .search(TC.caseInsensitiveContains(l.keys.toStream.sorted))
            .replace(_ + " ")
            .index(1)
        ))

    val abort: IO[Unit] =
      setState(None)

    val commit: Vector[A] => IO[Unit] =
      // TODO If change occurred, send to server & lock cell. (If unchanged, clear state.)
      s => setState(None) >>> IO{ println("Sent to ze server: " + s) }

    lazy val update: S => IO[Unit] =
      s => setState(Some(newState(s)))

    def newState(s: S) =
      new CellState(lookup, autoComplete, s, update, abort, commit)

    newState(init)
  }

  final class CellState(lookup      : Rx[Lookup],
                        autoComplete: AutoComplete,
                        state       : S,
                        stateUpdate : S => IO[Unit],
                        abort       : IO[Unit],
                        commit      : Vector[A] => IO[Unit]) extends Cell.Editing {

    def parse(s: S): ParseResult[A] =
      lookup.value().get(s) match {
        case Some(id) => \/-(id)
        case None     => leftNone
      }

    override def render = {
      val p = editor.Props(state, stateUpdate, abort, parse, commit, autoComplete)
      editor.component(p)
    }
  }
}
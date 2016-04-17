package shipreq.webapp.base.text

import japgolly.univeq.UnivEq
import org.parboiled2._
import scalaz.{Applicative, Monoid, \/, \/-, -\/}
import shipreq.base.util.{Backwards, Direction, Forwards}
import shipreq.webapp.base.data.{Project, Requirements, UseCaseStepId}
import shipreq.webapp.base.util.ParsingUtil

/**
 * Use case step titles have textual representations of step-flow, eg: "Try again if failed. --> 1.0.3".
 *
 * This deals with the flow portion.
 */
object UseCaseStepFlowText {

  sealed abstract class Elem[+T, +S] {
    final def bimap[F[_], TT, SS](f: T => F[TT], g: S => F[SS])(implicit F: Applicative[F]): F[Elem[TT, SS]] =
      this match {
        case Elem.Text(text) => F.map(f(text))(Elem.Text(_))
        case Elem.Step(step) => F.map(g(step))(Elem.Step(_))
        case a: Elem.Arrow   => F.point(a)
      }

    final def mapT[F[_], TT, SS >: S](f: T => F[TT])(implicit F: Applicative[F]): F[Elem[TT, SS]] =
      bimap[F, TT, SS](f, F.point(_))

    final def mapS[F[_], TT >: T, SS](f: S => F[SS])(implicit F: Applicative[F]): F[Elem[TT, SS]] =
      bimap[F, TT, SS](F.point(_), f)

    /*
    final def mapT[F[_], TT, SS >: S](f: T => F[TT])(implicit F: Applicative[F]): F[Elem[TT, SS]] =
      this match {
        case Elem.Text(text) => F.map(f(text))(Elem.Text(_))
        case s: Elem.Step[S] => F.point(s)
        case a: Elem.Arrow   => F.point(a)
      }

    final def mapS[F[_], TT >: T, SS](f: S => F[SS])(implicit F: Applicative[F]): F[Elem[TT, SS]] =
      this match {
        case Elem.Step(step) => F.map(f(step))(Elem.Step(_))
        case t: Elem.Text[T] => F.point(t)
        case a: Elem.Arrow   => F.point(a)
      }
    */
  }

  object Elem {
    sealed abstract class Flow[+S] extends Elem[Nothing, S]

    case class Text[+T](text: T)     extends Elem[T, Nothing]
    case class Step[+S](step: S)     extends Flow[S]
    case class Arrow(dir: Direction) extends Flow[Nothing]

    implicit def univEq[T: UnivEq, S: UnivEq]: UnivEq[Elem[T, S]] = UnivEq.force
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  /**
    * @return `Text` is never an empty string.
    *         `Text`s are never consecutive.
    *         `Step`s are always preceded by an `Arrow` or another `Step`.
    *         `Step`s never contain whitespace.
    *         Property tests enforce the above0.
    */
  def parse(input: String): Seq[Elem[String, String]] =
    new TextAndFlowParser(input).main.run()(Parser.DeliveryScheme.Throw)

  private final class TextAndFlowParser(val input: ParserInput) extends ParsingUtil {
    import ParsingUtil._

    /** Optional Single-Line WhiteSpace */
    def OSLWS: Rule0 =
      rule(SLWS.*)

    def arrowF: Rule1[Forwards.type] =
      rule('-' ~ ch('-').+ ~ '>' ~ push(Forwards))

    def arrowB: Rule1[Backwards.type] =
      rule("<-" ~ ch('-').+ ~ push(Backwards))

    def arrow: Rule1[Elem.Arrow] =
      rule(
        !lastCharIs(PunctuationOrSymbol) ~
        (arrowF | arrowB) ~
        !PunctuationOrSymbol
        ~> Elem.Arrow)

    def step: Rule1[Elem.Step[String]] =
      rule(capture(NonWhitespace.+) ~> (Elem.Step(_: String)))

    def flowClause: Rule1[Seq[Elem.Flow[String]]] =
      rule(
        arrow ~ OSLWS ~ ((arrow | step) ~ OSLWS).*
        ~> ((a: Elem.Arrow, t: Seq[Elem.Flow[String]]) => a +: t))

    type E = Elem[String, String]
    type ES = Seq[E]

    def line: Rule1[ES] =
      rule(!EOI ~ capture((!EOI ~ !arrow ~ ANY).*) ~ flowClause.? ~> flattenLine)

    val flattenLine: (String, Option[Seq[Elem.Flow[String]]]) => ES =
      (t, f) => {
        var r: ES = f.getOrElse(Nil)
        if (t.nonEmpty)
          r = Elem.Text(t) +: r
        r
      }

    def main: Rule1[ES] =
      rule(line.* ~ EOI ~> ((_: Seq[ES]).flatten))
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

//  def parseStep(reqs: Requirements)(step: String): Option[UseCaseStepId] =
//    new StepParser(reqs, step).useCaseStepLabel.run()(Parser.DeliveryScheme.Try).toOption

  /** @return The input, `step`, on the left, not an error message. */
  def parseStep(reqs: Requirements)(step: String): String \/ UseCaseStepId =
    new StepParser(reqs, step).useCaseStepLabel.run()(Parser.DeliveryScheme.Either) match {
      case Right(id) => \/-(id)
      case Left(_)   => -\/(step)
    }

  private final class StepParser(val reqs: Requirements, val input: ParserInput) extends Parsers.UseCaseStepLabel {
    override def OWS = rule(SLWS.*)
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  // String
  // parse -> Seq[Elem[String, String]]

  // 1. render - Seq[Elem[T.NonEmptyText, String \/ UseCaseStepId]]
  // 2. update - Option[ Seq[Elem[T.NonEmptyText, UseCaseStepId]] ]
  //             Option[ T.OptionalText, Dir => Set[UseCaseStepId] ]

//  1: Text.UseCaseStep.NonEmptyText
/*
  def renderable1[T <: Text.Generic](p: Project, t: T)(e: Elem[String, String]): Option[Elem[t.NonEmptyText, String \/ UseCaseStepId]] =
    e.bimap(
      t.parseNonEmpty(p)(_),
      parseStep(p.reqs)(_).some)

//  def renderable[T <: Text.Generic](p: Project, t: T)(e: TraversableOnce[Elem[String, String]]): Iterator[Elem[t.NonEmptyText, String \/ UseCaseStepId]] =
*/
  case class TextAndFlow[T, S](text: T, flow: Direction => S)

  def separateTextAndFlow[T, S](es: TraversableOnce[Elem[T, S]])(implicit M: Monoid[T]): TextAndFlow[T, Vector[S]] = {
    var t = M.zero
    var fwd = Vector.empty[S]
    var bck = Vector.empty[S]
    var dir: Direction = null
    es foreach {
      case Elem.Text(text) => t = M.append(t, text); dir = null
      case Elem.Arrow(d)   => dir = d
      case Elem.Step(step) => dir match {
        case Forwards  => fwd :+= step
        case Backwards => bck :+= step
      }
    }
    TextAndFlow(t, {
      case Forwards  => fwd
      case Backwards => bck
    })
  }

}

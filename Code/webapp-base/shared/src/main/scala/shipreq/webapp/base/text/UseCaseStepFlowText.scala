package shipreq.webapp.base.text

import japgolly.univeq.UnivEq
import org.parboiled2._
import shipreq.base.util.{Backwards, Direction, Forwards}
import shipreq.webapp.base.util.ParsingUtil

/**
 * Use case step titles have textual representations of step-flow, eg: "Try again if failed. --> 1.0.3".
 *
 * This deals with the flow portion.
 */
object UseCaseStepFlowText {

  sealed abstract class Elem[+T, +S]
  object Elem {
    sealed abstract class Flow[+S] extends Elem[Nothing, S]

    case class Text[+T](text: T)     extends Elem[T, Nothing]
    case class Step[+S](step: S)     extends Flow[S]
    case class Arrow(dir: Direction) extends Flow[Nothing]

    implicit def univEq[T: UnivEq, S: UnivEq]: UnivEq[Elem[T, S]] = UnivEq.force
  }

  private final class TextAndFlowParser(val input: ParserInput) extends ParsingUtil {
    import ParsingUtil._

    /** Single-Line WhiteSpace char */
    def SLWS: Rule0 =
      rule(!EOL ~ Whitespace)

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
        arrow ~ SLWS.* ~ ((arrow | step) ~ SLWS.*).*
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

  def parse(input: String): Seq[Elem[String, String]] =
    new TextAndFlowParser(input).main.run()(Parser.DeliveryScheme.Throw)

}

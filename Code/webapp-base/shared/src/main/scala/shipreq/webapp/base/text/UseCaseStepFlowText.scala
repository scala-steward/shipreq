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

//  def parseText(text: String)
  // split into lines, return

  // [line1] [<--] [1.0.2] [1.] [-->] [1.0.2] [1.] [\n line2 \n line3] [-->] [what]

  sealed abstract class Elem[+T, +S]
  object Elem {
    case class Text[+T](text: T)     extends Elem[T, Nothing]

    sealed abstract class Flow[+S] extends Elem[Nothing, S]

    case class Step[+S](step: S)     extends Flow[S]
    case class Arrow(dir: Direction) extends Flow[Nothing]

//    case class Flow[+S](dir: Direction, steps: List[S]) extends Elem[Nothing, S]

    implicit def univEq[T: UnivEq, S: UnivEq]: UnivEq[Elem[T, S]] = UnivEq.force
  }

//  private val clauseRegex =
//    s"^(.*?)${RegexUtil.PunctuationOrSymbol notAround "(<-{2,}|-{2,}>)"}(.*?)(?=[\r\n]|$$)".r
//
//  def parseText(text: String): Iterator[Elem[String, String]] =
//    if (text.isEmpty)
//      Iterator.empty
//    else {
//      val m = clauseRegex.pattern.matcher(text)
//      if (m.matches()) {
//
//        val pre    = m.group(1) + m.group(2) // #2 is in .notAround
//        val arrow  = m.group(3)
//        val clause = m.group(4)
//        val post   = if (m.hitEnd()) "" else text.substring(m.end())
//
//        var elems: List[Elem[String, String]] =
//          Elem.Arrow(Backwards <~ arrow(0) == '<') ::
//          RegexUtil.WhitespaceChars.split(clause).iterator.map(Elem.Step(_)).toList
//
//        if (pre.nonEmpty)
//          elems ::= Elem.Text(pre)
//
//        elems.toIterator ++ parseText(post)
//
//      } else
//        Iterator.single(Elem.Text(text))
//    }

  def parse(input: String): Seq[Elem[String, String]] =
    new TextAndFlowParser(input).main.run()(Parser.DeliveryScheme.Throw)

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
}

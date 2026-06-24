package shipreq.webapp.member.project.formula

import org.parboiled2.{Parser => _, _}
import scala.util.{Failure, Success, Try}
import shipreq.webapp.base.util._
import shipreq.webapp.member.project.formula.Formula.Potential
import shipreq.webapp.member.project.util.ParsingUtil

object FormulaParser {

  val preProcessor = PreProcessor.singleLine

  def parse(input: String): Result =
    parsePreProcessed(preProcessor(input))

  def parsePreProcessed(input: PreProcessed): Result = {
    val parser = new FormulaParser(input.charArray)
    parseResult(parser.main.run(), parser)
  }

  private def parseResult[A](t: Try[A], parser: FormulaParser): Failure \/ A =
    t match {
      case Success(a)             => \/-(a)
      case Failure(e: ParseError) => -\/(ParseException(e, parser.formatError(e, _)))
      case Failure(e: Throwable)  => -\/(GeneralException(e))
    }

  type Result = Failure \/ Potential

  sealed trait Failure
  final case class GeneralException(t: Throwable) extends Failure
  final case class ParseException(error: ParseError, formatter: ErrorFormatter => String) extends Failure {
    def format: String =
      format(new ErrorFormatter())
    def format(ef: ErrorFormatter): String =
      formatter(ef)
  }
}

// =====================================================================================================================

private[formula] class FormulaParser(val input: ParserInput) extends ParsingUtil {
  import ParsingUtil._

  private def OWS = rule(zeroOrMore(Whitespace))

  override def double: Rule1[Double] =
    rule(
      capture('-'.? ~ CharPredicate.Digit.+ ~ optional('.' ~ CharPredicate.Digit.*) ~ optional(anyOf("eE") ~ anyOf("+-").? ~ CharPredicate.Digit.+))
      ~> toDoubleOption ~ popOptional[Double]
    )

  private def literal: Rule1[Potential] = {
    def literalBool: Rule1[Potential] =
      rule(
        ignoreCase("true") ~ push(Potential.value(FormulaValue.Bool(true))) |
        ignoreCase("false") ~ push(Potential.value(FormulaValue.Bool(false)))
      )

    def literalDouble: Rule1[Potential] =
      rule(double ~> ((d: Double) => Potential.value(FormulaValue.Dbl(d))))

    def literalString: Rule1[Potential] = {
      def escapedQuote: Rule1[String] = rule("\"\"" ~ push("\""))
      def normalChar: Rule1[String] = rule(capture(noneOf("\"")))
      rule(
        '"' ~
        zeroOrMore(escapedQuote | normalChar) ~> ((chars: Seq[String]) => chars.mkString) ~
        '"' ~> ((s: String) => Potential.value(FormulaValue.Str(s)))
      )
    }

    rule(literalBool | literalDouble | literalString)
  }

  private def parens: Rule1[Potential] =
    rule('(' ~ OWS ~ expr ~ OWS ~ ')')

  private val fieldNameUnquotedChar =
    CharPredicate.from(c => (c != EOI) && FormulaAlgebra.isFieldNameUnquotedChar(c))

  private def field: Rule1[Potential] = {
    def quoteChar: Rule0 =
      rule('"')

    def name: Rule1[String] =
      rule(capture(fieldNameUnquotedChar.+) | (quoteChar ~ nonGreedyCapture(() => quoteChar)))

    rule("field:" ~ name ~> (Potential.field(_)))
  }

  private def function: Rule1[Potential] =
    rule(
      capture(CharPredicate.Alpha.+) ~ OWS ~ '('
      ~ zeroOrMore(OWS ~ expr).separatedBy(OWS ~ ',')
      ~ OWS ~ ')'
      ~> ((f: String, args: Seq[Potential]) => Potential.function(f.toUpperCase, args.toList))
    )

  private def factor: Rule1[Potential] =
    rule(field | literal | function | parens)

  private def term: Rule1[Potential] =
    rule(
      factor ~ zeroOrMore(OWS ~ (
        '*' ~ OWS ~ factor ~> (Potential.multiply(_, _)) |
        '/' ~ OWS ~ factor ~> (Potential.divide(_, _))
      ))
    )

  private def arith: Rule1[Potential] =
    rule(
      term ~ zeroOrMore(OWS ~ (
        '+' ~ OWS ~ term ~> (Potential.add(_, _)) |
        '-' ~ OWS ~ term ~> (Potential.subtract(_, _))
      ))
    )

  private def comparisonOp: Rule1[FormulaCmpOp] =
    rule(
      (("==" | "=")  ~ push(FormulaCmpOp.`=`)) |
      (("!=" | "<>") ~ push(FormulaCmpOp.!=)) |
      ((">=" | "≥")  ~ push(FormulaCmpOp.>=)) |
      ((">")         ~ push(FormulaCmpOp.>)) |
      (("<=" | "≤")  ~ push(FormulaCmpOp.<=)) |
      (("<")         ~ push(FormulaCmpOp.<))
    )

  private def possibleComparison: Rule1[Potential] =
    rule(
      arith ~ zeroOrMore(OWS ~ comparisonOp ~ OWS ~ arith ~> (Potential.compare(_, _, _)))
    )

  private def expr: Rule1[Potential] =
    rule(possibleComparison)

  def main: Rule1[Potential] =
    rule(
      EOI ~ push(Potential.value(FormulaValue.Empty))
      | expr ~ EOI)
}

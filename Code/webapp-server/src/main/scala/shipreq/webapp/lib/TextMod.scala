package shipreq.webapp.lib

import java.util.regex.Pattern
import scala.util.matching.Regex
import scalaz.Endo

object TextMod {

  def literal(from: Char, to: Char) = Endo[String](_.replace(from, to))
  def literal(from: String, to: String) = Endo[String](_.replace(from, to))

  def regex(regex: Regex, repl: String) = Endo[String](regex.replaceAllIn(_, repl))

  private[this] val punctuationOrSymbol = """[\p{S}\p{P}]"""
  def symbol(from: String, to: String) =
    regex(s"(?<!$punctuationOrSymbol)${Pattern quote from}(?!$punctuationOrSymbol)".r, to)

  // ---------------------------------------------------------------------------

  val trim = Endo[String](_.trim)

  val lowerCase = Endo[String](_.toLowerCase)

  val niceSymbols =
    symbol("<=", "≤") compose
    symbol(">=", "≥")

  val whitespaceRegex = "\\s+".r

  val singleLineWhitespace =
    regex(whitespaceRegex, " ") andThen trim

  val multiLineWhitespace =
    regex("\r\n?".r, "\n") andThen
    literal('\t', ' ') andThen
    trim

  val noWhitespace =
    regex(whitespaceRegex, "")

  val nonBlank: String => Option[String] =
    s => if (s.isEmpty) None else Some(s)
}

package com.beardedlogic.shipreq.feature.validation

import java.util.regex.Pattern
import scala.util.matching.Regex

trait TextReplacement {
  def apply(in: String): String
}

case class StaticRegexReplacement(regex: Regex, replacement: String) extends TextReplacement {
  override def apply(in: String): String = regex.replaceAllIn(in, replacement)
}

object TextReplacements {

  private [this] val PunctuationOrSymbol = """[\p{S}\p{P}]"""
  private def symbolReplacement(from: String, to: String): TextReplacement = {
    val f = Pattern.quote(from)
    StaticRegexReplacement(s"(?<!$PunctuationOrSymbol)$f(?!$PunctuationOrSymbol)".r, to)
  }

  val GeneralReplacements: List[TextReplacement] = List(
    symbolReplacement("<=", "≤"),
    symbolReplacement(">=", "≥")
  )

  @inline final def performGeneral(z: String): String =
    (z /: GeneralReplacements)((t, r) => r(t))
}

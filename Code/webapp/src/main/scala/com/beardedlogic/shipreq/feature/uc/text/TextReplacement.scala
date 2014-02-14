package com.beardedlogic.shipreq.feature.uc.text

import scala.util.matching.Regex

trait TextReplacement {
  def apply(in: String): String
}

case class StaticRegexReplacement(regex: Regex, replacement: String) extends TextReplacement {
  override def apply(in: String): String = regex.replaceAllIn(in, replacement)
}
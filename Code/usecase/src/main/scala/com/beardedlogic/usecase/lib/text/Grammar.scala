package com.beardedlogic.usecase.lib.text

import scala.annotation.tailrec
import scala.util.parsing.combinator.RegexParsers
import ParsingConfig._

/**
 * These parsers expresses the syntax that enables various special features to sprout from plain UC text.
 *
 * @since 15/05/2013
 */
object Grammar extends RegexParsers {

  // Gobbles whitespace. In plain-text we need whitespace preserved.
  @inline private def gobbleWhitespace(sb: StringBuilder, _in: Input) = {
    var in = _in
    while (!in.atEnd && Character.isWhitespace(in.first)) {
      sb += in.first
      in = in.rest
    }
    in
  }

  /**
   * Non-greedily matches 0-n characters, followed by another given matcher.
   *
   * (In-built parsers are all greedy; even ".*?".r won't work.)
   *
   * @param keepText If true, whitespace at the end of the text will be preserved.
   * @param nextParser The parser that matches after this. It must succeed for this to stop.
   * @tparam T The type of the next parser.
   * @return A tuple of the characters collected here (can be an empty string), and the result of nextParser.
   */
  def AnyTextThen[T](keepText: Boolean, nextParser: Parser[T]) = Parser[(String, T)] { in =>
    val sb = new StringBuilder
    @tailrec def parse(_in: Input): ParseResult[(String, T)] = {
      val in = if (keepText) gobbleWhitespace(sb, _in) else _in
      nextParser(in) match {
        case Success(a, rest) => Success((sb.toString, a), rest)
        case e@Error(_, _)    => e // still have to propagate error
        case _ if (in.atEnd)  => Failure("end of input", in)
        case _                => sb += in.first; parse(in.rest)
      }
    }
    parse(in)
  }

  /**
   * Non-greedily matches 0-n characters, optionally followed by another given matcher.
   *
   * In-built parsers are all greedy; even ".*?".r won't work.
   *
   * @param keepText If true, whitespace at the end of the text will be preserved.
   * @param nextParser The parser that may match after this. If it succeeds, this stops; else this will collect the
   *                   entire string.
   * @tparam T The type of the next parser.
   * @return A tuple of the characters collected here (can be an empty string), and the result of nextParser.
   */
  def AnyTextThenOptional[T](keepText: Boolean, nextParser: Parser[T]) = Parser[(String, Option[T])] { in =>
    val sb = new StringBuilder
    @tailrec def parse(_in: Input): ParseResult[(String, Option[T])] = {
      val in = if (keepText) gobbleWhitespace(sb, _in) else _in
      nextParser(in) match {
        case Success(a, rest) => Success((sb.toString, Some(a)), rest)
        case e@Error(_, _)    => e // still have to propagate error
        case _ if (in.atEnd)  => Success((sb.toString, None), in)
        case _                => sb += in.first; parse(in.rest)
      }
    }
    parse(in)
  }

  val StepLabelComponent: Parser[String] = "[A-Za-z]+|\\d+".r

  val StepLabel: Parser[String] = StepLabelComponent ~ rep1("." ~> StepLabelComponent) ^^ {
    case h ~ t => (h :: t).mkString(".")
  }

  val BracedRef: Parser[String] = "[" ~> StepLabel <~ "]"

  val OptionallyBracedRef: Parser[String] = BracedRef | StepLabel

  val FlowRefList: Parser[List[String]] = rep1sep(OptionallyBracedRef, "," ?)

  val FlowFromClause: Parser[List[String]] = FlowFromStyle.arrowRegex ~> FlowRefList

  val FlowToClause: Parser[List[String]] = FlowToStyle.arrowRegex ~> FlowRefList

  val TextAndFlow: Parser[(String, FlowParseResult)] =
    AnyTextThen(false,
      FlowFromClause ~ opt(FlowToClause) ^^ { case from ~ to => FlowParseResult(Some(from), to) } |
        FlowToClause ~ opt(FlowFromClause) ^^ { case to ~ from => FlowParseResult(from, Some(to)) }
    )

  /**
   * Matches Text and the first step reference. If no refs, then matches the entire input as Text.
   */
  val TextAndPossibleRef: Parser[(String, Option[String])] = AnyTextThenOptional(true, BracedRef)

  case class FlowParseResult(from: Option[List[String]], to: Option[List[String]])
}

package shipreq.webapp.base.validation

import java.util.regex.Pattern
import java.util.regex.Pattern.quote
import scala.util.matching.Regex
import shipreq.webapp.base.AppConsts
import Constraint._

object Constraints {
  implicit def regexToPattern(regex: Regex): Pattern = regex.pattern

  val nonEmpty = predicate[String](_.nonEmpty)("cannot be blank.")

  def matchesR(regex: Pattern) = predicate[String](regex.matcher(_).matches)

  def startsWithR(regex: String) = matchesR(s"^(?:$regex).*".r)

  def endsWithR(regex: String) = matchesR(s".*(?:$regex)$$".r)

  /** @param charRegex Like "a-zA-Z". No brackets. */
  def whitelistCharsR(charRegex: String) = matchesR(s"^[$charRegex]*$$".r)

  def whitelistCharsS(charList: String) = whitelistCharsR(quote(charList))

  def blacklistCharsR(charRegex: String) = matchesR(s"^[^$charRegex]*$$".r)

  def blacklistCharsS(charList: String) = blacklistCharsR(quote(charList))

  def containsR(regex: String) = matchesR(s".*$regex.*".r)

  /** Validates that a string contains at least one letter, and at least one number. */
  val containsAlphaAndNumber = matchesR(
    ".*?[A-Za-z].*?[0-9].*|.*?[0-9].*?[A-Za-z].*".r)(
    "must contain at least one letter, and at least one number.")

  /**
   * Validates that the length of a string is within min & max bounds.
   * @param range inclusive
   */
  def lengthInRange(range: Range) =
    predicate[String](range contains _.length)(
      s"must be between ${range.min} and ${range.max} characters long.")

  def maximumLength(max: Int) = Constraint.perf[String](
    _.length <= max // avoid creating errMsg if unneeded
    , s => {
      val excess = s.length - max
      if (excess > 0)
        s"is too large by $excess characters." :: Nil
      else
        Nil
    })

  val shortTextLimit = maximumLength(AppConsts.shortTextMaxLength)

  val largeTextLimit = maximumLength(AppConsts.largeTextMaxLength)

  val containsSurname = nonEmpty >> matchesR("""^\s*?\S+?\s+?\S.*""".r)("should include a surname, please.")

  def startsWithUpper        = startsWithR("[A-Z]"      )("must start with a capital letter.")
  def startsWithAlpha        = startsWithR("[A-Za-z]"   )("must start with a letter.")
  def startsWithAlphaNumeric = startsWithR("[A-Za-z0-9]")("must start with a letter or number.")

  def endsWithAlpha        = endsWithR("[A-Za-z]"   )("must end with a letter.")
  def endsWithAlphaNumeric = endsWithR("[A-Za-z0-9]")("must end with a letter or number.")
}

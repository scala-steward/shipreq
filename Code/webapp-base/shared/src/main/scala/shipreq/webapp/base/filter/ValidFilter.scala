package shipreq.webapp.base.filter

import japgolly.microlibs.adt_macros.AdtMacros
import japgolly.microlibs.nonempty.NonEmptyVector
import java.util.regex.Pattern
import scalaz.{-\/, \/, \/-}
import shipreq.base.util.Min2Set
import shipreq.base.util.univeq._
import shipreq.webapp.base.data

/**
 * A valid filter, ready to be applied to data.
 */
sealed trait ValidFilter

object ValidFilter {

  type Reqs = Set[data.ReqId] // If empty, then it's instant fail for the filter.

  sealed abstract class Attr(val name: String, val additionalNames: String*)
  object Attr {
    case object AnyIssue extends Attr("issues", "issue")
    case object AnyTag   extends Attr("tags", "tag")

    implicit def univEq: UnivEq[Attr] = UnivEq.derive

    val values: NonEmptyVector[Attr] =
      AdtMacros.adtValues[Attr]

    def availableText: String =
      values.whole.map(_.name).mkString(", ")

    val names: Map[String, Attr] =
      values.foldLeft(Map.empty[String, Attr])((m, a) =>
        a.additionalNames.foldLeft(m.updated(a.name, a))(_.updated(_, a)))

    def apply(n: String): Option[Attr] =
      names.get(n.toLowerCase)
  }

  case class Presence      (attr: Attr)                  extends ValidFilter
  case class Lack          (attr: Attr)                  extends ValidFilter
  case class ReqType       (id: data.ReqTypeId)          extends ValidFilter
  case class Tag           (id: data.ApplicableTagId)    extends ValidFilter
  case class CustomIssue   (id: data.CustomIssueTypeId)  extends ValidFilter
  case class Text          (substring: String)           extends ValidFilter
  case class ImpliesAnyOf  (reqs: Reqs)                  extends ValidFilter
  case class ImpliedByAnyOf(reqs: Reqs)                  extends ValidFilter
  case class AllOf         (inner: Min2Set[ValidFilter]) extends ValidFilter
  case class AnyOf         (inner: Min2Set[ValidFilter]) extends ValidFilter
  case class Not           (expr: ValidFilter)           extends ValidFilter

  case class TextPattern(pattern: Pattern) extends ValidFilter {
    override def hashCode = pattern.pattern.##
    override def equals(o: Any) = o match {
      case TextPattern(q) => (pattern.pattern ==* q.pattern) && (pattern.flags ==* q.flags)
      case _              => false
    }
  }
  implicit def univEqTextPattern: UnivEq[TextPattern] = UnivEq.force

  implicit def univEq: UnivEq[ValidFilter] = UnivEq.derive

  // -------------------------------------------------------------------------------------------------------------------

  def textPattern(regex: String): String \/ TextPattern =
    try
      \/-(TextPattern(Pattern compile regex))
    catch {
      // PatternSyntaxException not available in Scala.JS
      // case e: PatternSyntaxException => error(e.getDescription)
      case e: Throwable => -\/(s"Invalid regex: /$regex/")
    }

  /*
  def toSpec(p: data.Project, f: FilterAst): String \/ FilterSpec = {
    type R = String \/ FilterSpec
    implicit def mustToOpt(m: Must[FilterSpec]): R = m.fold(-\/.apply, \/-.apply)

    def byReqs(f: PF.Reqs => FilterSpec, reqs: Reqs): R = {
      val a: Must[Set[data.Req]] = p.reqs.data.reqsM(reqs)
      a.map(NonEmptySet.maybe(_, -\/("Empty <reqs>"): R)(rs =>
        rs.toStream.map(
      ))
    }

    def translateN(asts: NonEmptySet[FilterAst]): String \/ NonEmptySet[FilterSpec] =
      asts.traverseD(translate)

    def translate(f: FilterAst): R = {
      case Presence(a)          => PF.Presence(a.name)
      case Lack(a)              => PF.Lack(a.name)
      case ReqType(id)          => p.config.reqType        (id).map(r => PF.ReqType(r.mnemonic))
      case Tag(id)              => p.config.atag           (id).map(t => PF.HashRef(t.key))
      case CustomIssue(id)      => p.config.customIssueType(id).map(i => PF.HashRef(i.key))
      case TextPattern(pat)     => PF.Regex(pat.pattern)
      case ImpliesAnyOf(reqs)   => PF.Implies(reqs)
      case ImpliedByAnyOf(reqs) => PF.ImpliedBy(reqs)
      case AllOf(h, t)          => translateN(h +: t) map PF.AllOf
      case AnyOf(h, t)          => translateN(h +: t) map PF.AnyOf
      case Not(expr)            => translate(expr) map PF.Not

      case Text(t) =>
        def check(q: Char) = t.indexOf(q) >= 0
        (check('\''), check('"'), check('`'))  match {
          case (false, false, false) => PF.SimpleText(t)
          case (true , false, false) => PF.QuotedText(t, '\'')
          case (false, true , false) => PF.QuotedText(t, '"')
          case (false, false, true ) => PF.QuotedText(t, '`')
          case _ => -\/(s"No suitable quote character for [$t]")
        }
    }
    translate(f)
  }
  */
}

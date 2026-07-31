package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._
import japgolly.microlibs.utils.Memo
import shipreq.base.util._
import shipreq.webapp.member.project.data.DataImplicits._
import shipreq.webapp.member.project.data._

object FormulaEvalCache {

  sealed trait Issue
  object Issue {
    case object ReliesOnDeadNumberField  extends Issue
    case object ReliesOnOutOfRangeNumber extends Issue
    case object EvalError                extends Issue
  }

  final case class Eval(value: FormulaValue, live: Live, issues: Set[Issue]) {
    assert(live.is(Live) || issues.isEmpty, "Dead fields should not have issues.")

    val validity: Validity =
      Valid when issues.isEmpty
  }

  type Result = IfApplicable[Eval]

  def empty: FormulaEvalCache =
    new FormulaEvalCache(
      ProjectConfig.empty,
      ReqData.Numbers.empty,
      Map.empty
    )

  def fromProject(p: Project): FormulaEvalCache =
    new FormulaEvalCache(
      p.config,
      p.content.reqNums,
      p.content.reqs.reqTypeLookup
    )
}

final class FormulaEvalCache(cfg          : ProjectConfig,
                             reqNums      : ReqData.Numbers,
                             reqTypeLookup: Map[ReqId, ReqTypeId]
                             ) {

  import FormulaEvalCache._

  // `reqTypeLookup` is never used directly; it's just here for cache invalidation purposes.
  // Specifically, if a req changes req type we may need to re-calculate its formula.
  locally(reqTypeLookup)

  private final def memoByReqId = Memo.by[Req, ReqId](_.id)

  private val evalCache: CustomField.Formula.Id => Req => Result =
    Memo { fid =>
      val field     = cfg.fields.custom(fid)
      val liveField = field.live(cfg)

      memoByReqId { req =>
        field.fieldReqTypeRules(req.reqTypeId) match {

          case FieldReqTypeRules.Resolution.DefaultTo(formula) =>

            // Eval
            val algebra = FormulaAlgebra.eval(cfg.fields, reqNums, req)
            val value   = Recursion.cata(algebra)(formula.formula)

            // Issue detection
            var issues = Set.empty[Issue]
            val live = liveField & req.live(cfg.reqTypes)
            if (live is Live) {
              formula.fieldRefs.foreach {
                case FormulaFieldRef.NumberField(id) =>
                  val f = cfg.fields.custom(id)
                  if (f.live(cfg) is Dead)
                    issues += Issue.ReliesOnDeadNumberField
                  if (!reqNums.getVirtual(f, req).forall(f.isWithinRange))
                    issues += Issue.ReliesOnOutOfRangeNumber
              }
              if (value.isErr)
                issues += Issue.EvalError
            }

            // Done
            val result = Eval(value, live, issues)
            \/-(result)

          case FieldReqTypeRules.Resolution.Optional
             | FieldReqTypeRules.Resolution.Mandatory
             | FieldReqTypeRules.Resolution.NotApplicable =>
              NotApplicable.left
        }
      }
    }

  def apply(fid: CustomField.Formula.Id): Req => Result =
    evalCache(fid)
}

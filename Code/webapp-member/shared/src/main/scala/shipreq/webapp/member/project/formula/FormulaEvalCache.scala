package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._
import japgolly.microlibs.utils.Memo
import shipreq.base.util._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.data.DataImplicits._

object FormulaEvalCache {

  type Value = ErrorMsg \/ FormulaValue

  /** @param validity Invalid when the formula relies on dead fields.
    */
  final case class Eval(value: Value, live: Live, validity: Validity) {

    def isBlank: Boolean =
      value match {
        case \/-(FormulaValue.Empty) => true
        case _                       => false
      }

    def doubleOption: Option[Double] =
      value match {
        case \/-(FormulaValue.Dbl(d)) => Some(d)
        case _                        => None
      }
  }

  type Result = IfApplicable[Eval]

  def empty: FormulaEvalCache =
    new FormulaEvalCache(
      ProjectConfig.empty,
      ReqData.Numbers.empty,
      Map.empty
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

  private val validityCache: CustomField.Formula.Id => ReqTypeId => Validity =
    Memo { fid =>
      val field     = cfg.fields.custom(fid)
      val liveField = field.live(cfg)
      Memo { reqTypeId =>
        field.fieldReqTypeRules(reqTypeId) match {

          case FieldReqTypeRules.Resolution.DefaultTo(formula) =>
            liveField match {
              case Live =>
                Valid when formula.fieldRefs.forall {
                  case FormulaFieldRef.NumberField(id) => cfg.fields.custom(id).live(cfg) is Live
                }
              case Dead => Valid
            }

          case FieldReqTypeRules.Resolution.Optional
             | FieldReqTypeRules.Resolution.Mandatory
             | FieldReqTypeRules.Resolution.NotApplicable =>
              Valid
        }
      }
    }

  private val evalCache: CustomField.Formula.Id => Req => Result =
    Memo { fid =>
      val field     = cfg.fields.custom(fid)
      val liveField = field.live(cfg)
      val validityLookup = validityCache(fid)
      memoByReqId { req =>
        field.fieldReqTypeRules(req.reqTypeId) match {

          case FieldReqTypeRules.Resolution.DefaultTo(formula) =>
            val algebra  = FormulaAlgebra.eval(cfg.fields, reqNums, req)
            val value    = Recursion.cataM(algebra)(formula.formula)
            val live     = liveField & req.live(cfg.reqTypes)
            val validity = Invalid.when(value.isLeft) & validityLookup(req.reqTypeId)
            val result   = Eval(value, live, validity)
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

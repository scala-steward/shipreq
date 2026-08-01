package shipreq.webapp.member.project.formula

import japgolly.microlibs.testutil.TestUtil._
import shipreq.webapp.member.project.data.DataImplicits._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.test.project.UnsafeTypes._
import sourcecode.Line

object FormulaTestShared {

  val scoreFieldId = 1.CFNum
  val scoreField = CustomField.Number(
    id = scoreFieldId,
    name = "score",
    desc = None,
    range = (0.0, 100.0),
    decimalPlaces = 2,
    fieldReqTypeRules = FieldReqTypeRules.optional,
    liveExplicitly = Live
  )

  val bonusFieldId = 2.CFNum
  val bonusField = CustomField.Number(
    id = bonusFieldId,
    name = "bonus",
    desc = None,
    range = (0.0, 100.0),
    decimalPlaces = 1,
    fieldReqTypeRules = FieldReqTypeRules.optional.defaultTo(10.0)(CustomReqTypeId(1)),
    liveExplicitly = Live
  )

  val fieldSet = FieldSet(
    emptyDataMap(CustomField).add(scoreField).add(bonusField),
    Vector(scoreFieldId, bonusFieldId)
  )

  val reqId = GenericReqId(10)
  val req = GenericReq(reqId, PubidT(CustomReqTypeId(1), ReqTypePos(1)), ∅, Live)

  def parseOrDie(input: String)(implicit q: Line): Formula.Potential = {
    FormulaParser.parse(input) match {
      case \/-(f) => f
      case -\/(f: FormulaParser.ParseException) => fail(f.format)
      case -\/(f) => fail(f.toString)
    }
  }
}

package shipreq.webapp.member.project.formula

import shipreq.webapp.member.project.data.CustomField

sealed trait FormulaFieldRef

object FormulaFieldRef {
  final case class NumberField(id: CustomField.Number.Id) extends FormulaFieldRef
}

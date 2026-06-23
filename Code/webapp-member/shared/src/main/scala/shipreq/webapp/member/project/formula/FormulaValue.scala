package shipreq.webapp.member.project.formula

sealed trait FormulaValue

object FormulaValue {
  final case class Dbl (value: Double)  extends FormulaValue
  final case class Str (value: String)  extends FormulaValue
  final case class Bool(value: Boolean) extends FormulaValue

  implicit def univEq: UnivEq[FormulaValue] = UnivEq.derive
}

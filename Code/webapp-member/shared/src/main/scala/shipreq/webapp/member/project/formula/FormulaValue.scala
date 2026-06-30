package shipreq.webapp.member.project.formula

import shipreq.base.util.Util

sealed trait FormulaValue {
  def show: String
}

object FormulaValue {
  final case class Dbl(value: Double) extends FormulaValue {
    override def show = Util.doubleToString(value)
  }

  final case class Str(value: String) extends FormulaValue {
    override def show = value
  }

  final case class Bool(value: Boolean) extends FormulaValue {
    override def show = value.toString.toUpperCase
  }

  case object Empty extends FormulaValue {
    override def show = ""
  }

  implicit def univEq: UnivEq[FormulaValue] = UnivEq.derive
}

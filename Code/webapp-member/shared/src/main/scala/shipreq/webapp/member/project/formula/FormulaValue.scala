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

  implicit def univEq: UnivEq[FormulaValue] =
    UnivEq.derive

  val compare: (FormulaValue, FormulaValue) => Int = (x, y) =>
    if (x ==* y)
      0
    else
      (x, y) match {
        case (Dbl(a), Dbl(b))   => java.lang.Double.compare(a, b)
        case (Str(a), Str(b))   => a.compareTo(b)
        case (Bool(a), Bool(b)) => java.lang.Boolean.compare(a, b)
        case (Empty, Empty)     => 0
        case _ =>
          def priority(v: FormulaValue): Int = v match {
            case Empty   => 0
            case _: Bool => 1
            case _: Dbl  => 2
            case _: Str  => 3
          }
          priority(x) - priority(y)
      }
}

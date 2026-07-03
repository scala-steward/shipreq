package shipreq.webapp.member.project.formula

sealed trait FormulaValue {
  @inline final def isEmpty = this ==* FormulaValue.Empty
  def isErr = false
  def doubleOption: Option[Double] = None
}

object FormulaValue {

  final case class Dbl(value: Double) extends FormulaValue {
    override def doubleOption = Some(value)
  }

  final case class Str(value: String) extends FormulaValue

  final case class Err(value: String) extends FormulaValue {
    override def isErr = true
  }

  final case class Bool(value: Boolean) extends FormulaValue {
    def show = value.toString.toUpperCase
  }

  case object Empty extends FormulaValue

  implicit def univEq: UnivEq[FormulaValue] =
    UnivEq.derive

  val compare: (FormulaValue, FormulaValue) => Int = (x, y) =>
    if (x ==* y)
      0
    else
      (x, y) match {
        case (Dbl(a), Dbl(b))   => java.lang.Double.compare(a, b)
        case (Str(a), Str(b))   => a.compareTo(b)
        case (Err(a), Err(b))   => a.compareTo(b)
        case (Bool(a), Bool(b)) => java.lang.Boolean.compare(a, b)
        case _ =>
          def priority(v: FormulaValue): Int = v match {
            case Empty   => 0
            case _: Err  => 1
            case _: Str  => 2
            case _: Bool => 3
            case _: Dbl  => 4
          }
          priority(x) - priority(y)
      }
}

package shipreq.webapp.member.project.formula

sealed trait FormulaCmpOp

object FormulaCmpOp {
  case object `=` extends FormulaCmpOp
  case object !=  extends FormulaCmpOp
  case object >   extends FormulaCmpOp
  case object <   extends FormulaCmpOp
  case object >=  extends FormulaCmpOp
  case object <=  extends FormulaCmpOp

  implicit def univEq: UnivEq[FormulaCmpOp] = UnivEq.derive
}

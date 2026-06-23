package shipreq.webapp.member.project.formula

sealed abstract class FormulaCmpOp(final val symbol: String)

object FormulaCmpOp {
  case object `=` extends FormulaCmpOp("=")
  case object !=  extends FormulaCmpOp("<>")
  case object >   extends FormulaCmpOp(">")
  case object <   extends FormulaCmpOp("<")
  case object >=  extends FormulaCmpOp(">=")
  case object <=  extends FormulaCmpOp("<=")

  implicit def univEq: UnivEq[FormulaCmpOp] = UnivEq.derive
}

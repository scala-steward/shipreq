package shipreq.webapp.member.project.formula

import japgolly.microlibs.adt_macros.AdtMacros

sealed abstract class FormulaCmpOp(final val symbol: String)

object FormulaCmpOp {
  case object `=` extends FormulaCmpOp("=")
  case object !=  extends FormulaCmpOp("<>")
  case object >   extends FormulaCmpOp(">")
  case object <   extends FormulaCmpOp("<")
  case object >=  extends FormulaCmpOp(">=")
  case object <=  extends FormulaCmpOp("<=")

  lazy val all = AdtMacros.adtValues[FormulaCmpOp]

  implicit def univEq: UnivEq[FormulaCmpOp] = UnivEq.derive
}

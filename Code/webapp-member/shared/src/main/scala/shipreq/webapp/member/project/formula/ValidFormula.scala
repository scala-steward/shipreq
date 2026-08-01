package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._

final case class ValidFormula(formula: Formula.Valid) {

  lazy val fieldRefs: Set[FormulaFieldRef] =
    Recursion.cata(FormulaAlgebra.fieldRefs)(formula)
}

object ValidFormula {
  implicit def univEq: UnivEq[ValidFormula] =
    UnivEq.derive
}

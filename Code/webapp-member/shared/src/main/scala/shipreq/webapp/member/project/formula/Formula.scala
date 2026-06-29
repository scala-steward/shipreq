package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._
import shipreq.base.util.ErrorMsg
import shipreq.webapp.member.project.data.FieldSet

object Formula {

  type PotentialF[+F] = FormulaAst[
    F,
    Potential.Fn,
    Potential.Field,
  ]

  type Potential = Fix[PotentialF]

  object Potential extends FormulaAst.Dsl {
    override type Fn = String
    override type Field = String

    def toText(f: Potential): String =
      AtomOrComposite.cata(FormulaAlgebra.unparse)(f)

    def validate(pf: Potential, fieldSet: FieldSet): ErrorMsg \/ Formula.Valid =
      Recursion.cataM(FormulaAlgebra.validate(fieldSet))(pf)
  }

  // ===================================================================================================================

  type ValidF[+F] = FormulaAst[
    F,
    Valid.Fn,
    Valid.Field,
  ]

  type Valid = Fix[ValidF]

  object Valid extends FormulaAst.Dsl {
    override type Fn = FormulaFunction
    override type Field = FormulaFieldRef

    def toPotential(formula: Valid, fieldSet: FieldSet): Potential =
      Recursion.cata(FormulaAlgebra.unvalidate(fieldSet))(formula)

    def toText(formula: Valid, fieldSet: FieldSet): String =
      Potential.toText(toPotential(formula, fieldSet))
  }
}

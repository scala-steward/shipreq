package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._
import shipreq.base.util.ErrorMsg
import shipreq.webapp.member.project.data.{FieldSet, Req, ReqData}

object Formula {

  type Validator = FAlgebraM[ErrorMsg \/ *, PotentialF, Valid]

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

    def validate(pf: Potential, validator: Validator): ErrorMsg \/ Formula.Valid =
      Recursion.cataM[ErrorMsg \/ *, PotentialF, Valid](validator)(pf)
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

    def eval(formula: Valid, fieldSet: FieldSet, reqNums: ReqData.Numbers, req: Req): ErrorMsg \/ FormulaValue =
      Recursion.cataM(FormulaAlgebra.eval(fieldSet, reqNums, req))(formula)
  }
}

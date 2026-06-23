package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._

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
  }

}

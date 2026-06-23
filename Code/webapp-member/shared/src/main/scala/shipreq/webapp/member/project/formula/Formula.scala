package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._

object Formula {

  type PotentialF[+F] = FormulaAst[F]

  type Potential = Fix[PotentialF]

  object Potential extends FormulaAst.Dsl

}

package shipreq.webapp.member.project.formula

import japgolly.microlibs.adt_macros.AdtMacros

sealed abstract class FormulaFunction(final val name: String)

object FormulaFunction {
  case object If    extends FormulaFunction("IF")
  case object Not   extends FormulaFunction("NOT")
  case object Round extends FormulaFunction("ROUND")

  val all = AdtMacros.adtValues[FormulaFunction]

  val byName: Map[String, FormulaFunction] =
    all.iterator.map(f => (f.name, f)).toMap
}

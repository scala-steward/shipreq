package shipreq.webapp.member.project.formula

import japgolly.microlibs.adt_macros.AdtMacros

sealed abstract class FormulaFunction(final val name: String)

object FormulaFunction {
  case object And      extends FormulaFunction("AND")
  case object Average  extends FormulaFunction("AVERAGE")
  case object Ceiling  extends FormulaFunction("CEILING")
  case object Err      extends FormulaFunction("ERR")
  case object Floor    extends FormulaFunction("FLOOR")
  case object If       extends FormulaFunction("IF")
  case object IsBlank  extends FormulaFunction("ISBLANK")
  case object IsBool   extends FormulaFunction("ISBOOL")
  case object IsErr    extends FormulaFunction("ISERR")
  case object IsNumber extends FormulaFunction("ISNUMBER")
  case object IsText   extends FormulaFunction("ISTEXT")
  case object Max      extends FormulaFunction("MAX")
  case object Min      extends FormulaFunction("MIN")
  case object Not      extends FormulaFunction("NOT")
  case object Or       extends FormulaFunction("OR")
  case object Round    extends FormulaFunction("ROUND")

  val all = AdtMacros.adtValues[FormulaFunction]

  implicit def univEq: UnivEq[FormulaFunction] =
    UnivEq.derive

  val byName: Map[String, FormulaFunction] =
    all.iterator.map(f => (f.name, f)).toMap
}

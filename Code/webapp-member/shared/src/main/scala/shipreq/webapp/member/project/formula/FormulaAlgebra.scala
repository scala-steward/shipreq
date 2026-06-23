package shipreq.webapp.member.project.formula

object FormulaAlgebra {
  val isFieldNameUnquotedChar: Char => Boolean = {
    case ':' | '=' | '"' | ')' | '<' | '>' | '≥' | '≤' => false
    case c => !c.isWhitespace
  }
}

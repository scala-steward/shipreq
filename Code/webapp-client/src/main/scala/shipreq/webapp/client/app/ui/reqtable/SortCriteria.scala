package shipreq.webapp.client.app.ui.reqtable

sealed trait SortCriterion
object SortCriterion {
  case class Inconclusive(column: Column.SortInconclusive, method: SortMethod             ) extends SortCriterion
  case class Conclusive  (column: Column.SortConclusive,   method: SortMethod.IgnoreBlanks) extends SortCriterion
}

case class SortCriteria(init: Vector[SortCriterion.Inconclusive], last: SortCriterion.Conclusive) {
//  def removeColumnI(c: Column.SortInconclusive): SortCriteria =
//    copy(init = init.filterNot(_.column ≟ c))
//
//  def removeColumn: Column => SortCriteria = {
//    case c: Column.SortInconclusive => removeColumnI(c)
//    case _: Column.SortConclusive   => this
//  }

  def whitelistColumns(w: Set[Column.SortInconclusive]): SortCriteria =
    copy(init = init.filter(w contains _.column))
}

object SortCriteria {

  val default =
    SortCriteria(
      Vector(
        SortCriterion.Inconclusive(Column.Code,  SortMethod.AscThenBlanks)),
      SortCriterion.Conclusive    (Column.PubId, SortMethod.Asc))
}
package shipreq.webapp.client.app.ui.reqtable

import monocle.macros.Lenses
import scalaz.syntax.equal._
import shipreq.base.util.ScalaExt._
import shipreq.base.util.{NonEmptyVector, UnivEq}

sealed trait SortCriterion {
  def column: Column
  def method: SortMethod
  def reverse: SortCriterion
}

object SortCriterion {
  import Column.{NoBlanks, HasBlanks, SortConclusive, SortInconclusive}
  import SortMethod.{ConsiderBlanks, IgnoreBlanks}

  sealed trait Inconclusive extends SortCriterion {
    override def column: SortInconclusive
    override def reverse: Inconclusive
  }

  case class InconclusiveCB(column: SortInconclusive with HasBlanks, method: ConsiderBlanks) extends Inconclusive {
    override def reverse: InconclusiveCB = copy(method = this.method.reverse)
  }

  case class InconclusiveIB(column: SortInconclusive with NoBlanks,  method: IgnoreBlanks)   extends Inconclusive {
    override def reverse: InconclusiveIB = copy(method = this.method.reverse)
  }

  case class Conclusive(column: SortConclusive, method: IgnoreBlanks) extends SortCriterion  {
    override def reverse: Conclusive = copy(method = this.method.reverse)
  }

  implicit def equalityIIB: UnivEq[InconclusiveIB] = UnivEq.derive
  implicit def equalityICB: UnivEq[InconclusiveCB] = UnivEq.derive
  implicit def equalityI  : UnivEq[Inconclusive]   = UnivEq.derive
  implicit def equalityC  : UnivEq[Conclusive]     = UnivEq.derive
  implicit def equality   : UnivEq[SortCriterion]  = UnivEq.derive

  def possibilitiesICB(c: SortInconclusive with HasBlanks): NonEmptyVector[InconclusiveCB] =
    SortMethod.considerBlanks.map(InconclusiveCB(c, _))

  def possibilitiesIIB(c: SortInconclusive with NoBlanks): NonEmptyVector[InconclusiveIB] =
    SortMethod.ignoreBlanks.map(InconclusiveIB(c, _))

  def possibilitiesI(c: SortInconclusive): NonEmptyVector[Inconclusive] = c match {
    case d: SortInconclusive with HasBlanks => possibilitiesICB(d)
    case d: SortInconclusive with NoBlanks  => possibilitiesIIB(d)
  }

  // Syntax Helpers
  @inline implicit class SortCriterionExt1(private val c: SortInconclusive with HasBlanks) extends AnyVal {
    @inline def /(sm: ConsiderBlanks) = InconclusiveCB(c, sm)
  }
  @inline implicit class SortCriterionExt2(private val c: SortInconclusive with NoBlanks) extends AnyVal {
    @inline def /(sm: IgnoreBlanks) = InconclusiveIB(c, sm)
  }
  @inline implicit class SortCriterionExt3(private val c: SortConclusive) extends AnyVal {
    @inline def /(sm: IgnoreBlanks) = Conclusive(c, sm)
  }
}

import SortCriterion._

@Lenses
case class SortCriteria(init: Vector[Inconclusive], last: Conclusive) {
//  def removeColumnI(c: Column.SortInconclusive): SortCriteria =
//    copy(init = init.filterNot(_.column ≟ c))
//
//  def removeColumn: Column => SortCriteria = {
//    case c: Column.SortInconclusive => removeColumnI(c)
//    case _: Column.SortConclusive   => this
//  }

  def whitelistColumns(w: Set[Column.SortInconclusive]): SortCriteria =
    copy(init = init.filter(w contains _.column))

  def reverse: SortCriteria =
    SortCriteria(init.map(_.reverse), last.reverse)

  def isOrdered(c: Column): Boolean =
    isOrdered(_ ≟ c)

  def isOrdered(f: Column => Boolean): Boolean =
    f(last.column) || isOrderedI(f)

  def isOrderedI(c: Column.SortInconclusive): Boolean =
    isOrderedI(_ ≟ c)

  def isOrderedI(f: Column.SortInconclusive => Boolean): Boolean =
    init.exists(_.column |> f)

  def filterColumns(f: Column => Boolean): SortCriteria = {
    val i = init.filter(s => f(s.column))
    val c = if (f(last.column)) last else SortCriteria.defaultConclusive
    SortCriteria(i, c)
  }

  /**
   * The user "wants" this column, in the context of sort criteria.
   * The user's desire has been delivered through a very limited information stream consisting only of a column,
   * (i.e. usually indicated by a click on a column name), and so without additional information to help interpret
   * our masters' desire, we apply some rules:
   *
   * 1. User wants column to be primary sort column.
   * 2. If column is already the primary sort column, user wants to change its direction.
   */
  def want(column: Column): SortCriteria = {
    import SortMethod._

    def next[A: UnivEq](as: Vector[A])(a: A): A =
      as((as.indexOf(a) + 1) % as.length)

    column match {
      case c: Column.SortInconclusive =>
        val newInit =
          if (init.headOption.exists(_.column ≟ c)) {
            // Column(I) already primary
            val h = init.head match {
              case ex: InconclusiveCB => ex.column / next(considerBlanks.whole)(ex.method)
              case ex: InconclusiveIB => ex.column / next(ignoreBlanks  .whole)(ex.method)
            }
            h +: init.tail
          } else init.find(_.column ≟ c) match {
            case Some(existing) =>
              //  Column(I) exists but isn't primary
              existing +: init.filterNot(_ eq existing)
            case None =>
              //  Column(I) is new
              val h = c match {
                case c2: Column.HasBlanks => c2 / considerBlanks.head
                case c2: Column.NoBlanks  => c2 / ignoreBlanks  .head
              }
              h +: init
          }
        SortCriteria(newInit, last)

      case c: Column.SortConclusive =>
        val newLast =
          if (c ≟ last.column) {
            if (init.isEmpty)
              // Column(C) already primary
              c / next(ignoreBlanks.whole)(last.method)
            else
              // Column(C) exists but isn't primary
              last
          } else
            // Column(C) change
            c / ignoreBlanks.head
        SortCriteria(Vector.empty, newLast)
    }
  }
}

object SortCriteria {
  implicit def equality: UnivEq[SortCriteria] = UnivEq.derive

  val defaultConclusive =
    Column.Pubid / SortMethod.Asc

  def byPubidOnly =
    SortCriteria(Vector.empty, defaultConclusive)

  val default = SortCriteria(
    Vector(Column.Code / SortMethod.AscThenBlanks),
    defaultConclusive)
}
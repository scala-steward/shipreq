package shipreq.webapp.base.sort

import scalajs.js.{Array => JArray}
import Sorter._

final class FusedSorters[Setup, Row](init: Vector[Sorter[Setup, Row]], last: Sorter[Setup, Row]) extends Sorter[Setup, Row] {
  private[this] val ss       = init :+ last
  private[this] val tSize    = ss.size + 1
  private[this] val rowIndex = tSize - 1

  // Unsafe and mutable.
  // [d₁, …, dₙ, row] where n = ss.size
  override type T = JArray[Any]

  override val prepFn: PrepFn[Setup, Row, T] =
    p => {
      val eachPrepFn: JArray[Row => Any] =
        JArray(ss.map(_ prepFn p): _*)

      row => {
        val t: T = new JArray[Any](tSize)
        var i = 0
        eachPrepFn.foreach { f =>
          t(i) = f(row)
          i = i + 1
        }
        t(i) = row
        t
      }
    }

  override val rowModFn: RowModFn[Setup, Row] =
    consolidateRowModFns(ss.iterator.map(_.rowModFn))

  private def eachSortFn: Vector[(T, T) => Int] =
    ss.zipWithIndex.map {
      case (s, i) =>
        val f = s.sortFn.f
        (as: T, bs: T) => {
          val a = as(i).asInstanceOf[s.T]
          val b = bs(i).asInstanceOf[s.T]
          f(a, b)
        }
    }

  override val sortFn: SortFn[T] =
    SortFn(eachSortFn.reduce { (s, t) =>
      val f = s
      val g = t
      (as: T, bs: T) => {
        val r = f(as, bs)
        if (r == 0) g(as, bs) else r
      }
    })

  def row(t: T): Row =
    t(rowIndex).asInstanceOf[Row]
}

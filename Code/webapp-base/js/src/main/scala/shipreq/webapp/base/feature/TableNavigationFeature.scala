package shipreq.webapp.base.feature


import japgolly.univeq._
import japgolly.microlibs.stdlib_ext.StdlibExt._
import japgolly.scalajs.react.{raw => _, _}
import scala.annotation.tailrec
import scalajs.js
import org.scalajs.dom.{Element, html}
import scalaz.{-\/, \/, \/-}
import shipreq.base.util.Util
import shipreq.webapp.base.lib.DomUtil.{TableCellZipper => _, _}

object TableNavigationFeature {

  final case class PosXY(x: Int, y: Int)
  object PosXY {
    implicit def univEq: UnivEq[PosXY] = UnivEq.derive
  }

  final case class TablePos(body: Int, row: Int, cell: Int, sub: Option[PosXY])
  object TablePos {
    implicit def univEq: UnivEq[TablePos] = UnivEq.derive
  }

  sealed trait Axis
  object Axis {
    case object UpDown extends Axis
    case object LeftRight extends Axis
  }

  type ParentStream = Stream[(html.Element, Int)]

  def parentsAndIndices(e: html.Element): ParentStream =
    Option(e.parentElement) match {
      case Some(p) => Stream((e, siblingIndex(e))) append parentsAndIndices(p)
      case None    => Stream((e, 0))
    }

  private implicit class HtmlElementExtX(private val e: html.Element) extends AnyVal {
    def child(i: Int): String \/ html.Element =
      if (e.children.isEmpty)
        -\/(s"No children found: ${e.outerHTML}")
      else {
        val j = Util.fitCollectionIndex(i, e.children.length)
        \/-(e.children(j).domAsHtml)
      }
  }

  final case class TableCellZipper(focus: html.Element) {
    //  @inline implicit private def autoCastHtml(e: Element) = e.domAsHtml

    type F[A] = String \/ A

    private lazy val parentStream =
      parentsAndIndices(focus)

    //  lazy val (tableRoot, pos): (html.Element, TablePos) =
    //    ???

    private lazy val stuff =
      findStuff(0)

    def root: F[html.Table] =
      stuff.map(_._1)

    def focusPos: F[TablePos] =
      stuff.map(_._2)

    private def findStuff(innerElements: Int): F[(html.Table, TablePos)] =
      parentStream.drop(innerElements).map(_._1.tagName) match {

        case ("TD" | "TH") #:: "TR" #:: ("TBODY" | "THEAD") #:: "TABLE" #:: _ =>
          val (td #:: tr #:: tbody #:: table #:: _) = parentStream
          val subAttempt: String \/ Option[PosXY] =
            if (innerElements ==* 0)
              \/-(None)
            else
              ???
//              innerRep(parentStream.take(innerElements))
          //          parentStream.take(innerElements) match {
          //            case Stream.Empty => \/-(None)
          //            case _ => -\/("Unable to determine cell sub-structure")
          //          }
          subAttempt.map(sub =>
            (table._1.domCast[html.Table], TablePos(tbody._2, tr._2, td._2, sub)))

        case _ #:: _ =>
          findStuff(innerElements + 1)

        case Stream.Empty =>
          -\/("Unable to determine table structure")
      }

    def goto(pos: TablePos): F[TableCellZipper] =
      for {
        table <- root
        tbody <- table.child(pos.body)
        tr    <- tbody.child(pos.row)
        td    <- tr   .child(pos.cell)
      } yield TableCellZipper(td)

//    def pos(a: Axis, m: Movement): F[TablePos] =
//      focusPos.flatMap(pos =>
//        a match {
//          case Axis.LeftRight =>
//            for {
//              table <- root
//              tbody <- table.child(pos.body)
//              tr    <- tbody.child(pos.row)
//            } yield {
//              //tr.children.iterator.zipWithIndex.map(_.map1(_.domAsHtml)).fil
//              val xs = tr.children.iterator.focusable.toVector
//              val i = xs.indexWhere(_ eq focus)
//              assert(i >= 0) // TODO
//              val j = Util.fitCollectionIndex(m adjustIndex i, xs.length)
//              val e = xs(j)
//              ???
//            }
////            TablePos(p.body, p.row, m adjustIndex p.cell, None)
//        }
//      )

    def move(a: Axis, m: Movement): F[TableCellZipper] =
//      pos(a, m).flatMap(goto)
      focusPos.flatMap(pos =>
        a match {
          case Axis.LeftRight =>
            for {
              table <- root
              tbody <- table.child(pos.body)
              tr    <- tbody.child(pos.row)
            } yield {
              //tr.children.iterator.zipWithIndex.map(_.map1(_.domAsHtml)).fil
              val xs = tr.children.iterator.focusable.toVector
              val i = xs.indexWhere(_ eq focus)
              assert(i >= 0) // TODO
              val j = Util.fitCollectionIndex(m adjustIndex i, xs.length)
              val e = xs(j)
//              println(s"$a $m $i --> $j = ${e.outerHTML}")
              TableCellZipper(e)
            }
//            TablePos(p.body, p.row, m adjustIndex p.cell, None)
        }
      )

  }
}

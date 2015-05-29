package shipreq.webapp.client.test

import org.scalajs.dom.html
import shipreq.base.util.UnivEq.{apply => _, force => _, _}
import shipreq.webapp.base.test.BaseTestUtil._

class DomZipper(root: Sizzle.DOM) {
  import Sizzle.{DOM, Result}

  def getAll(sel: String): Result =
    Sizzle(sel, root)

  def getAll(expectCount: Int, sel: String): Result = {
    val r = Sizzle(sel, root)
    assertEq(sel + r.map("\n" + _.outerHTML.take(160)), expectCount, r.length)
    r
  }

  def apply(cssSel: String): DomZipper =
    apply(1, cssSel, 0)

  def apply(expectedCount: Int, cssSel: String, selectIndex: Int): DomZipper = {
    val n = getAll(expectedCount, cssSel)(selectIndex)
    new DomZipper(n)
  }

  def collect[A](sel: String, f: DOM => A): Vector[A] =
    getAll(sel).foldLeft(Vector.empty[A])(_ :+ f(_))

  def collectD[A](sel: String, f: DomZipper => A): Vector[A] =
    collect(sel, d => f(new DomZipper(d)))

  def collectInnerHTML[A](sel: String): Vector[String] =
    collect(sel, _.innerHTML)

  def get: DOM =
    root

  def as[D <: DOM]: D =
    root.asInstanceOf[D]

  def innerHTML: String =
    root.innerHTML

  def selectedOptionText: String = {
    val s = as[html.Select]
    s.options(s.selectedIndex).innerHTML
  }

  def inputChecked: Boolean =
    as[html.Input].checked

//    def collect2[A, B](selA: String, a: DOM => A)(selB: String, b: DOM => B): Vector[(A, B)] = {
//      val as = collect(selA, a)
//      val bs = collect(selB, b)
//      assertEq(as.length, bs.length)
//      as zip bs
//    }
}

package shipreq.webapp.client.test

import org.scalajs.dom.{Element, Node, window}
import org.scalajs.dom.html
import scala.reflect.ClassTag
import scala.scalajs.js
import DomZipper.{CssSelLookup, DOM, HndDown, Layer, MofN, Sole}
import DomZipper.Implicits.removeReactIds

object DomZipper {
  type DOM = Node

  type Temp = DomZipper[DOM] // TODO DELETE

  type CssSelLookup = (String, Node) => js.Array[Element]

  trait DomLike[-A] {
    type D <: DOM
    def apply(a: A): D
  }
  type DomLikeAux[-A, D2 <: DOM] = DomLike[A] {type D = D2}
  def DomLike[A, D2 <: DOM](f: A => D2): DomLikeAux[A, D2] =
    new DomLike[A] {
      override type D = D2
      override def apply(a: A) = f(a)
    }

  trait HndDown {
    type Result[A]
    def pass[A](a: A): Result[A]
    def fail[A](e: String): Result[A]
    def apply[A](e: Either[String, A]): Result[A] =
      e match {
        case Right(a) => pass(a)
        case Left(s) => fail(s)
      }
  }

  // ===================================================================================================================
  // Implicits that should be modular

  object Implicits {
    implicit val cssSelLookupSizzle: CssSelLookup =
      Sizzle(_, _)

    import japgolly.scalajs.react._
    implicit def domFromReact[D <: TopNode with DOM]: DomLikeAux[CompScope.Mounted[D], D] =
      DomLike(ReactDOM.findDOMNode)

    /*
    implicit object UseScalazEither extends EitherLike {
      import scalaz._
      override type Or[L, R] = L \/ R
      override def left[L, R](l: L)                               = -\/(l)
      override def right[L, R](r: R)                              = \/-(r)
      override def fold[L, R, O](x: L Or R)(l: L => O, r: R => O) = x.fold(l, r)
    }
    */

    implicit object JustThrow extends HndDown {
      override type Result[A]         = A
      override def pass[A](a: A)      = a
      override def fail[A](e: String) = sys error removeReactIds(e)
    }

    def removeReactIds(html: String): String = // TODO Remove
      html.replaceAll(""" data-reactid=".*?"""", "")
  }

  // ===================================================================================================================

  def showNameSel(name: String, sel: String): String =
    (Option(name).filter(_.nonEmpty), Option(sel).filter(_.nonEmpty)) match {
      case (Some(n), Some(s)) => s"$n [$s]"
      case (None   , Some(s)) => s
      case (Some(n), None   ) => n
      case (None   , None   ) => "?"
    }

  case class Layer[+D <: DOM](name: String, sel: String, dom: D) {
    def show = showNameSel(name, sel)
  }

  def root(implicit $: CssSelLookup): DomZipper[DOM] =
    new DomZipper[DOM](Vector.empty, Layer("window.document", "", window.document), $)

  def apply[A](tgt: A)(implicit domLike: DomLike[A], $: CssSelLookup): DomZipper[domLike.D] =
    apply("<manual>", tgt)

  def apply[A](name: String, tgt: A)(implicit domLike: DomLike[A], $: CssSelLookup): DomZipper[domLike.D] =
    new DomZipper(Vector.empty, Layer(name, "", domLike(tgt)), $)

  case class MofN(m: Int, n: Int) {
    override def toString = s"$m of $n"
    assert(n > 0, s"$this is invalid. $n must be > 0.")
    assert(m > 0, s"$this is invalid. $m must be > 0.")
    assert(m <= n, s"$this is invalid. $m must be ≤ $n.")
  }

  implicit class IntExt(private val i: Int) extends AnyVal {
    def of(n: Int) = MofN(i, n)
  }

  val Sole = 1 of 1
}

final class DomZipper[+D <: DOM] private[test](prevLayers: Vector[Layer[DOM]], curLayer: Layer[D], $: CssSelLookup) {

  val dom: D =
    curLayer.dom

  def as_![D2 <: DOM]: DomZipper[D2] =
    this.asInstanceOf[DomZipper[D2]]

  def as[D2 <: DOM](implicit ct: ClassTag[D2]): Option[DomZipper[D2]] =
    getDom[D2].map(d2 => new DomZipper(prevLayers, curLayer.copy(dom = d2), $))

  def getDom_![D2 <: DOM]: D2 =
    dom.asInstanceOf[D2]

  def getDom[D2 <: DOM](implicit ct: ClassTag[D2]): Option[D2] =
    ct.unapply(dom)

  def dynamicMethod[A](f: js.Dynamic => Any): Option[A] =
    f(dom.asInstanceOf[js.Dynamic]).asInstanceOf[js.UndefOr[A]].toOption

  def dynamicString(f: js.Dynamic => Any): String =
    dynamicMethod(f) getOrElse "<undefined>"

  def outerHTML: String = removeReactIds(dynamicString(_.outerHTML)) // TODO removeReactIds
  def innerHTML: String = removeReactIds(dynamicString(_.innerHTML)) // TODO removeReactIds
  def innerText: String = dom.textContent
  def value: String = dynamicString(_.value)

  def downE(sel: String): Either[String, DomZipper[DOM]] =
    downE("", sel)

  def downE(sel: String, which: MofN): Either[String, DomZipper[DOM]] =
    downE("", sel, which)

  def downE(name: String, sel: String): Either[String, DomZipper[DOM]] =
    downE(name, sel, Sole)

  def downE(name: String, sel: String, which: MofN): Either[String, DomZipper[DOM]] = {
    val results = $(sel, dom)
    if (results.length != which.n)
      Left {
        val q = Option(name).filter(_.nonEmpty).fold("Q")(_ + " q")
        failMsg(s"${q}uery failed: [$sel]. Expected ${which.n} results, not ${results.length}.")
      }
    else {
      val nextLayer = Layer(name, sel, results(which.m - 1))
      Right(addLayer(nextLayer))
    }
  }

  def downO(sel: String)                           : Option[DomZipper[DOM]] = downE(sel)             .right.toOption
  def downO(sel: String, which: MofN)              : Option[DomZipper[DOM]] = downE(sel, which)      .right.toOption
  def downO(name: String, sel: String)             : Option[DomZipper[DOM]] = downE(name, sel)       .right.toOption
  def downO(name: String, sel: String, which: MofN): Option[DomZipper[DOM]] = downE(name, sel, which).right.toOption

  def down(sel: String)                           (implicit h: HndDown): h.Result[DomZipper[DOM]] = h(downE(sel)             )
  def down(sel: String, which: MofN)              (implicit h: HndDown): h.Result[DomZipper[DOM]] = h(downE(sel, which)      )
  def down(name: String, sel: String)             (implicit h: HndDown): h.Result[DomZipper[DOM]] = h(downE(name, sel)       )
  def down(name: String, sel: String, which: MofN)(implicit h: HndDown): h.Result[DomZipper[DOM]] = h(downE(name, sel, which))

//  def down_!(sel: String)                           (implicit e: HndDown): DomZipper = need_!(e)(down(sel))
//  def down_!(sel: String, which: MofN)              (implicit e: HndDown): DomZipper = need_!(e)(down(sel, which))
//  def down_!(name: String, sel: String)             (implicit e: HndDown): DomZipper = need_!(e)(down(name, sel))
//  def down_!(name: String, sel: String, which: MofN)(implicit e: HndDown): DomZipper = need_!(e)(down(name, sel, which))

  private def addLayer[D2 <: DOM](nextLayer: Layer[D2]) =
    new DomZipper(prevLayers :+ curLayer, nextLayer, $)

  def allLayers =
    prevLayers :+ curLayer

  def describe: String =
    s"DESC: ${allLayers.map(_.show) mkString " → "}\nHTML: $outerHTML"

  private def failMsg(msg: String): String =
    msg + "\n" + describe

  // ======= hmmmm… =======

  def getAll(sel: String) =
    $(sel, dom)

  def collectDom[A](sel: String, f: DOM => A): Vector[A] =
    getAll(sel).foldLeft(Vector.empty[A])(_ :+ f(_))

  def collect[A](sel: String, f: DomZipper[DOM] => A): Vector[A] =
    collectDom(sel, d => f(addLayer(Layer("collect", sel, d))))

  def collectInnerHTML[A](sel: String): Vector[String] =
    collect(sel, _.innerHTML)

  def collectInnerText[A](sel: String): Vector[String] =
    collectDom(sel, _.textContent)

  // ======= hmmmm… =======

  def getAll1(sel: String) = {
    val x = getAll(sel)
    assert(x.nonEmpty, s"No matches found for $sel") // TODO nooo.....
    x
  }

  def collectDom1[A](sel: String, f: DOM => A): Vector[A] =
    getAll1(sel).foldLeft(Vector.empty[A])(_ :+ f(_))

  def collect1[A](sel: String, f: DomZipper[DOM] => A): Vector[A] =
    collectDom1(sel, d => f(addLayer(Layer("collect", sel, d))))

  def collectInnerHTML1[A](sel: String): Vector[String] =
    collect1(sel, _.innerHTML)

  def collectInnerText1[A](sel: String): Vector[String] =
    collectDom1(sel, _.textContent)

  // ======= hmmmm… =======

  // TODO Use dep types for return types.
  // inputChecked

  def inputChecked: Option[Boolean] =
    getDom[html.Input].map(_.checked)

  /** The currently selected option in a &lt;select&gt; dropdown. */
  def selectedOption: Option[html.Option] =
    getDom[html.Select].flatMap(s =>
      if (s.selectedIndex >= 0)
        Some(s.options(s.selectedIndex))
      else
        None
    )

  /** The text value of the currently selected option in a &lt;select&gt; dropdown. */
  def selectedOptionText: Option[String] =
    selectedOption.map(_.text)
}

//  def assertCount(desc: String, expectedCount: Int, dom: Result, root: UndefOr[DOM]): Unit = {
//    def showDom(inner: Boolean)(d: DOM) = {
//      val html = if (inner) d.innerHTML else d.outerHTML
//      "\n" + removeReactIds(html).take(160)
//    }
//    def detail =
//      if (dom.isEmpty)
//        root.fold("")(showDom(true))
//      else
//        dom.map(showDom(false))
//    assertEq(desc + detail, dom.length, expectedCount)
//  }
//
//  def first(desc: String, dom: Result): DomZipper = {
//    if (dom.isEmpty)
//      fail(desc + ": empty")
//    new DomZipper(dom.head)
//  }
//
//  implicit val equality: Equal[DomZipper] =
//    Equal.equal((a, b) => a.get isSameNode b.get)
//}
//
//  def getAll(expectedCount: Int, sel: String): Result = {
//    val r = Sizzle(sel, root)
//    DomZipper.assertCount(sel, expectedCount, r, root)
//    r
//  }

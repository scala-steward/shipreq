package shipreq.webapp.client.test

import org.scalajs.dom.{Element, Node, window}
import org.scalajs.dom.html
import scala.reflect.ClassTag
import scala.scalajs.js
import DomZipper.{CssSelLookup, DOM, EitherLike, Layer, MofN, Sole}

object DomZipper {
  type DOM = Node

  type CssSelLookup = (String, Node) => js.Array[Element]

  trait DomLike[A] {
    def apply(a: A): DOM
  }
  def DomLike[A](f: A => DOM): DomLike[A] =
    new DomLike[A] {
      override def apply(a: A) = f(a)
    }

  trait EitherLike {
    type Or[L, R]
    def left[L, R](l: L): L Or R
    def right[L, R](r: R): L Or R
    def fold[L, R, O](x: L Or R)(l: L => O, r: R => O): O
  }

  // ===================================================================================================================
  // Implicits that should be modular

  object Implicits {
    implicit val cssSelLookupSizzle: CssSelLookup =
      Sizzle(_, _)

    import japgolly.scalajs.react._
    implicit def domFromReact[A <: CompScope.Mounted[TopNode]]: DomLike[A] =
      DomLike(ReactDOM.findDOMNode)

    implicit object UseScalazEither extends EitherLike {
      import scalaz._
      override type Or[L, R] = L \/ R
      override def left[L, R](l: L)                               = -\/(l)
      override def right[L, R](r: R)                              = \/-(r)
      override def fold[L, R, O](x: L Or R)(l: L => O, r: R => O) = x.fold(l, r)
    }
  }

  // ===================================================================================================================

  case class Layer(name: String, sel: String, dom: DOM)

  def root(implicit $: CssSelLookup): DomZipper =
    new DomZipper(Vector.empty[Layer] :+ Layer("window.document", "", window.document), $)

  def apply[A](tgt: A)(implicit domLike: DomLike[A], $: CssSelLookup): DomZipper =
    apply("<manual>", tgt)

  def apply[A](name: String, tgt: A)(implicit domLike: DomLike[A], $: CssSelLookup): DomZipper =
    new DomZipper(Vector.empty[Layer] :+ Layer(name, "?", domLike(tgt)), $)

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

final class DomZipper private[test](layers: Vector[Layer], $: CssSelLookup) {
  assert(layers.nonEmpty)

  val dom: DOM =
    layers.last.dom

  def to_![D <: DOM]: D =
    dom.asInstanceOf[D]

  def to[D <: DOM](implicit ct: ClassTag[D]): Option[D] =
    ct.unapply(dom)

  def outerHTML: String = to[html.Element].fold("")(_.outerHTML)
  def innerHTML: String = to[html.Element].fold("")(_.innerHTML)
  def innerText: String = dom.textContent

  def down(sel: String)(implicit e: EitherLike): e.Or[String, DomZipper] =
    down("…", sel)

  def down(sel: String, which: MofN)(implicit e: EitherLike): e.Or[String, DomZipper] =
    down("…", sel, which)

  def down(name: String, sel: String)(implicit e: EitherLike): e.Or[String, DomZipper] =
    down(name, sel, Sole)

  def down(name: String, sel: String, which: MofN)(implicit e: EitherLike): e.Or[String, DomZipper] = {
    val results = $(sel, dom)
    if (results.length != which.n)
      e.left(failMsg(s"Expected ${which.n} results, got ${results.length}."))
    else {
      val nextLayer = Layer(name, sel, results(which.m - 1))
      e.right(addLayer(nextLayer))
    }
  }

  private def addLayer(nextLayer: Layer) =
    new DomZipper(layers :+ nextLayer, $)

//  def downO(sel: String)                           : Option[DomZipper] = down(sel)             .toOption
//  def downO(sel: String, which: MofN)              : Option[DomZipper] = down(sel, which)      .toOption
//  def downO(name: String, sel: String)             : Option[DomZipper] = down(name, sel)       .toOption
//  def downO(name: String, sel: String, which: MofN): Option[DomZipper] = down(name, sel, which).toOption

  def down_!(sel: String)                           (implicit e: EitherLike): DomZipper = need_!(e)(down(sel))
  def down_!(sel: String, which: MofN)              (implicit e: EitherLike): DomZipper = need_!(e)(down(sel, which))
  def down_!(name: String, sel: String)             (implicit e: EitherLike): DomZipper = need_!(e)(down(name, sel))
  def down_!(name: String, sel: String, which: MofN)(implicit e: EitherLike): DomZipper = need_!(e)(down(name, sel, which))

  def describe: String =
    s"Desc: ${layers.map(_.name) mkString " → "}\nPath: ${layers.map(_.sel) mkString " → "}\nHTML: $outerHTML"

  private def failMsg(msg: String): String =
    msg + "\n" + describe

  private def need_!(e: EitherLike)(x: e.Or[String, DomZipper]): DomZipper =
    e.fold[String, DomZipper, DomZipper](x)(sys.error, identity)

  def collectDom[A](sel: String, f: DOM => A): Vector[A] =
    $(sel, dom).foldLeft(Vector.empty[A])(_ :+ f(_))

  def collect[A](sel: String, f: DomZipper => A): Vector[A] =
    collectDom(sel, d => f(addLayer(Layer("collect", sel, d))))

  def collectInnerHTML[A](sel: String): Vector[String] =
    collect(sel, _.innerHTML)

  def collectInnerText[A](sel: String): Vector[String] =
    collectDom(sel, _.textContent)

  def inputChecked: Option[Boolean] =
    to[html.Input].map(_.checked)

  /** The currently selected option in a &lt;select&gt; dropdown. */
  def selectedOption: Option[html.Option] =
    to[html.Select].flatMap(s =>
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
//  def removeReactIds(html: String): String =
//    html.replaceAll(""" data-reactid=".*?"""", "")
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

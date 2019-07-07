package shipreq.webapp.base.sort

import japgolly.microlibs.stdlib_ext.MutableArray
import monocle.Optional
import scala.annotation.tailrec
import scala.reflect.ClassTag
import scalaz.std.option.optionInstance
import shipreq.base.util.ScalaExt._

trait Sorter[Setup, Row] { self =>
  import Sorter._

  type T
  def prepFn  : PrepFn[Setup, Row, T]
  def sortFn  : SortFn[T]
  def rowModFn: RowModFn[Setup, Row]

  def reverse: Sorter[Setup, Row] =
    new Sorter[Setup, Row] {
      override type T                              = self.T
      override def prepFn  : PrepFn[Setup, Row, T] = self.prepFn
      override val sortFn  : SortFn[T]             = self.sortFn.reverse
      override def rowModFn: RowModFn[Setup, Row]  = self.rowModFn.map(f => (s, dir) => f(s, dir.flip))
    }
}

object Sorter {
  import SortMethod.{Asc, AscThenBlanks, BlanksThenAsc}

  /** Extracts, pre-processes and normalises data before sorting. */
  type PrepFn[-Setup, -Row, +A] = Setup => Row => A

  /** Sorts values in Expansion and MultiValues. */
  type RowModFn[Setup, Row]   = Option[(Setup, Dir) => EndoFn[Row]]

  def apply[Setup, Row, A](prep: PrepFn[Setup, Row, A], sort: SortFn[A], rowMod: Sorter.RowModFn[Setup, Row] = None): Sorter[Setup, Row] =
    new Sorter[Setup, Row] {
      override type T       = A
      override val prepFn   = prep
      override val sortFn   = sort
      override val rowModFn = rowMod
    }

  class WithTypes[Setup, Row] {
    import shipreq.webapp.base.sort.{Sorter => S}

    final type Sorter        = S[Setup, Row]
    final type PrepFn[+T]    = S.PrepFn[Setup, Row, T]
    final type RowModFn      = S.RowModFn[Setup, Row]
    final type SorterForSMIB = S.SorterForSMIB[Setup, Row]
    final type SorterForSMCB = S.SorterForSMCB[Setup, Row]

    @inline final def sorter[A](prep: PrepFn[A], sort: SortFn[A], rowMod: RowModFn = None): Sorter = S(prep, sort, rowMod)
    @inline final def sorterForSMIB(s: Sorter) = S.SorterForSMIB(s)
    @inline final def sorterForSMCB(f: BlankPlacement => Sorter) = S.SorterForSMCB(f)
  }

  sealed trait BlankPlacement
  case object BlanksFirst extends BlankPlacement
  case object BlanksLast  extends BlankPlacement

  @inline implicit def autoBlanksFirst(a: BlanksThenAsc.type): BlankPlacement = BlanksFirst
  @inline implicit def autoBlanksLast (a: AscThenBlanks.type): BlankPlacement = BlanksLast

  sealed trait Dir {
    def flip: Dir
    def apply[A](a: A)(r: A => A): A
  }

  case object KeepDir extends Dir {
    override def flip = FlipDir
    override def apply[A](a: A)(r: A => A): A = a
  }

  case object FlipDir extends Dir {
    override def flip = KeepDir
    override def apply[A](a: A)(r: A => A): A = r(a)
  }

  // ===================================================================================================================
  // SortFn

  final case class SortFn[A](f: (A, A) => Int) {
    @inline def apply(x: A, y: A) = f(x, y)

    def &&&[B](next: SortFn[B]): SortFn[(A, B)] = {
      val g = next.f
      SortFn { (x, y) =>
        val a = f(x._1, y._1)
        if (a == 0)
          g(x._2, y._2)
        else
          a
      }
    }

    def option(bp: BlankPlacement): SortFn[Option[A]] =
      considerBlanksF[Option[A]](_.isEmpty)(_.get)(bp)

    def vector(bp: BlankPlacement): SortFn[Vector[A]] = {
      @tailrec def go(as: Vector[A], bs: Vector[A]): Int = {
        val ea = as.isEmpty
        val eb = bs.isEmpty
        if (ea) {
          if (eb) 0 else -1
        } else {
          if (eb) 1 else {
            val r = f(as.head, bs.head)
            if (r == 0) go(as.tail, bs.tail) else r
          }
        }
      }
      SortFn(go).considerBlanks(_.isEmpty)(bp)
    }

    def pair: SortFn[(A, A)] =
      this &&& this

    def byBlankPlacement[B](f: SortFn[A] => BlankPlacement => SortFn[B]): BlankPlacement => SortFn[B] = {
      val bf = f(this)(BlanksFirst)
      val bl = f(this)(BlanksLast)
      ;{
        case BlanksFirst => bf
        case BlanksLast  => bl
      }
    }

    def considerBlanks(isBlank: A => Boolean)(bp: BlankPlacement): SortFn[A] =
      considerBlanksF(isBlank)(identity)(bp)

    def considerBlanksF[B](isBlank: B => Boolean)(nonBlank: B => A)(bp: BlankPlacement): SortFn[B] = {
      val (headBlank, tailBlank) = bp match {
        case BlanksFirst => (-1, 1)
        case BlanksLast  => (1, -1)
      }
      SortFn { (x, y) =>
        val bx = isBlank(x)
        val by = isBlank(y)
        if (bx) {
          if (by) 0 else headBlank
        } else
          if (by) tailBlank else f(nonBlank(x), nonBlank(y))
      }
    }

    def contramap[B](g: B => A): SortFn[B] =
      SortFn((x, y) => f(g(x), g(y)))

    def applyDir(dir: Dir): SortFn[A] =
      dir(this)(_.reverse)

    def reverse: SortFn[A] =
      SortFn((x, y) => -f(x, y))

    def toOrdering: Ordering[A] =
      new Ordering[A] {
        def compare(x: A, y: A): Int = f(x, y)
      }
  }

  object SortFn {
    val int: SortFn[Int] =
      SortFn(_ - _)

    val intPair: SortFn[(Int, Int)] =
      int.pair

    val intVector: BlankPlacement => SortFn[Vector[Int]] =
      int.byBlankPlacement(_.vector)

    val intPairVector: BlankPlacement => SortFn[Vector[(Int, Int)]] =
      intPair.byBlankPlacement(_.vector)

    val stringNonEmpty: SortFn[String] =
      SortFn(_ compareTo _)

    val string: BlankPlacement => SortFn[String] =
      stringNonEmpty.byBlankPlacement(_.considerBlanks(_.isEmpty))
  }

  // ===================================================================================================================
  // General

  type SorterForSMIB[Setup, Row] = SortMethod.IgnoreBlanks   => Sorter[Setup, Row]
  type SorterForSMCB[Setup, Row] = SortMethod.ConsiderBlanks => Sorter[Setup, Row]

  def SorterForSMIB[Setup, Row](s: Sorter[Setup, Row]): SorterForSMIB[Setup, Row] =
    SortMethod.resolverIB{ case Asc => s }(_.reverse)

  def SorterForSMCB[Setup, Row](f: BlankPlacement => Sorter[Setup, Row]): SorterForSMCB[Setup, Row] =
    SortMethod.resolverCB({
      case b@ AscThenBlanks => f(b)
      case b@ BlanksThenAsc => f(b)
    })(_.reverse)

  private def tryModEndo[A, B](l: Optional[A, B])(mod: B => Option[B]): EndoFn[A] =
    a => l.modifyF[Option](mod)(a) getOrElse a

  def typicalRowModFn[Setup, Row, A: ClassTag, B](l: Optional[Row, Vector[A]], s: SortFn[B])(f: Setup => A => B): RowModFn[Setup, Row] =
    Some((setup, dir) => {
      val n = f(setup)
      val o = s.applyDir(dir).toOrdering
      def innerSort(i: Vector[A]): Option[Vector[A]] =
        if (i.isEmpty || i.tail.isEmpty)
          None
        else
          MutableArray(i).sortBySchwartzian(n)(o).to[Vector].some
      tryModEndo(l)(innerSort)
    })

  def consolidateRowModFns[Setup, Row](ss: TraversableOnce[RowModFn[Setup, Row]]): RowModFn[Setup, Row] = {
    val fns = ss.toIterator.flatMap(a => a).toList
    if (fns.isEmpty)
      None
    else
      Some((setup, dir) => row => fns.foldLeft(row)((r, f) => f(setup, dir)(r)))
  }
}
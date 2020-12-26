package shipreq.base.util

import scala.runtime.AbstractFunction2
import PartialOrder.Cmp

/** Laws:
  *   - reflexivity
  *   - antisymmetry
  *   - transitivity
  */
final class PartialOrder[-A](asFn: (A, A) => Cmp) extends AbstractFunction2[A, A, Cmp] {
  override def apply(a: A, b: A): Cmp =
    asFn(a, b)
}

object PartialOrder {

  def apply[A](f: (A, A) => Cmp): PartialOrder[A] =
    new PartialOrder(f)

  sealed trait Cmp {
    import Cmp._

    final def flip: Cmp =
      this match {
        case Lesser  => Greater
        case Greater => Lesser
        case x       => x
      }
  }

  object Cmp {
    case object Equal    extends Cmp
    case object Lesser   extends Cmp
    case object Greater  extends Cmp
    case object Separate extends Cmp

    def keyedInt[A, K: UnivEq](key: A => K, value: A => Int): PartialOrder[A] =
      PartialOrder((x, y) =>
        if (key(x) != key(y))
          Separate
        else {
          val q = value(x) - value(y)
          if (q == 0)
            Equal
          else if (q > 0)
            Greater
          else
            Lesser
        }
      )

    implicit def univEq: UnivEq[Cmp] = UnivEq.derive
  }

  object ImplicitOps {
    import Cmp._

    implicit class PartialOrderOps[A](private val lhs: A) extends AnyVal {

      @inline def >=(rhs: A)(implicit po: PartialOrder[A]): Boolean =
        po(lhs, rhs) match {
          case Greater | Equal => true
          case _               => false
        }

      @inline def <=(rhs: A)(implicit po: PartialOrder[A]): Boolean =
        po(lhs, rhs) match {
          case Lesser | Equal => true
          case _              => false
        }

      @inline def <(rhs: A)(implicit po: PartialOrder[A]): Boolean =
        po(lhs, rhs) == Lesser

      @inline def >(rhs: A)(implicit po: PartialOrder[A]): Boolean =
        po(lhs, rhs) == Greater

      @inline def isComparableTo(rhs: A)(implicit po: PartialOrder[A]): Boolean =
        po(lhs, rhs) != Separate

      @inline def isSeparateTo(rhs: A)(implicit po: PartialOrder[A]): Boolean =
        po(lhs, rhs) == Separate
    }

  }
}
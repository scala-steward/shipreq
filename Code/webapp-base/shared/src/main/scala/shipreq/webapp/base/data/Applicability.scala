package shipreq.webapp.base.data

import shipreq.base.util.IsoBool
import Applicability.Subject

sealed trait Applicable extends IsoBool.WithBoolOps[Applicable] {
  override final def companion = Applicable
}

case object Applicable extends Applicable with IsoBool.Object[Applicable] {
  override def positive = Applicable
  override def negative = NotApplicable
}

case object NotApplicable extends Applicable

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

sealed abstract class Applicability {

  def apply[A](a: A)(implicit s: Subject[A]): Applicable

  def fn[A, B](f: A => B)(`n/a`: B)(implicit s: Subject[A]): A => B
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object Applicability {

  def const(a: Applicable): Applicability =
    a match {
      case Applicable    => Always
      case NotApplicable => Never
    }

  def fn[A](lookup: A => Option[ReqTypeId => Applicable], default: Applicable): A => Applicability = {
    val fallback = const(default)
    a => lookup(a) match {
      case Some(f) => new Depends(f)
      case None    => fallback
    }
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  object Always extends Applicability {
    override def apply[A](a: A)(implicit s: Subject[A]) =
      Applicable

    override def fn[A, B](f: A => B)(`n/a`: B)(implicit s: Subject[A]) =
      f
  }

  object Never extends Applicability {
    override def apply[A](a: A)(implicit s: Subject[A]) =
      NotApplicable

    override def fn[A, B](f: A => B)(`n/a`: B)(implicit s: Subject[A]) =
      _ => `n/a`
  }

  final class Depends(reqTypeFilter: ReqTypeId => Applicable) extends Applicability {
    override def apply[A](a: A)(implicit s: Subject[A]) =
      s.applicable(a, reqTypeFilter)

    override def fn[A, B](f: A => B)(`n/a`: B)(implicit s: Subject[A]) =
      a => this(a) match {
        case Applicable    => f(a)
        case NotApplicable => `n/a`
      }
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  trait Subject[A] {
    def applicable(a: A, reqTypeFilter: ReqTypeId => Applicable): Applicable
  }

  implicit object SubjectReq extends Subject[Req] {
    override def applicable(r: Req, reqTypeFilter: ReqTypeId => Applicable) =
      reqTypeFilter(r.reqTypeId)
  }
}

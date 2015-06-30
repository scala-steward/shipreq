package shipreq.webapp.base.util

import shapeless._
import shipreq.base.util.UnivEq
import scalaz.Equal

// ---------------------------------------------------------------------------------------------------------------------
// Low priority

trait TypeclassDerivation_LowPri {

  implicit def _deriveEqual_HCons[H, T <: HList](implicit eh: Lazy[Equal[H]], et: Lazy[Equal[T]]): Equal[H :: T] =
    new Equal[H :: T] {
      override def equalIsNatural = eh.value.equalIsNatural && et.value.equalIsNatural
      override def equal(a: H :: T, b: H :: T) = {
        @inline def head = eh.value.equal(a.head, b.head)
        @inline def tail = et.value.equal(a.tail, b.tail)
        head && tail
      }
    }

  implicit def _deriveEqual_CCons[H, T <: Coproduct](implicit eh: Lazy[Equal[H]], et: Lazy[Equal[T]]): Equal[H :+: T] =
    new Equal[H :+: T] {
      override def equalIsNatural = eh.value.equalIsNatural && et.value.equalIsNatural
      override def equal(a: H :+: T, b: H :+: T) =
        (a, b) match {
          case (Inl(x), Inl(y)) => eh.value.equal(x, y)
          case (Inr(x), Inr(y)) => et.value.equal(x, y)
          case _ => false
        }
    }

  def deriveEqual[F, G](implicit gen: Generic.Aux[F, G], e: Lazy[Equal[G]]): Equal[F] =
    new Equal[F] {
      override def equalIsNatural = e.value.equalIsNatural
      override def equal(a: F, b: F) = e.value.equal(gen.to(a), gen.to(b))
    }
}

trait TypeclassDerivation_LowPri_Auto {
  implicit def deriveEqual[F, G](implicit gen: Generic.Aux[F, G], e: Lazy[Equal[G]]): Equal[F] =
    TypeclassDerivation.deriveEqual[F, G]
}

// ---------------------------------------------------------------------------------------------------------------------
// Top priority

object TypeclassDerivation extends TypeclassDerivation_LowPri {

  @inline implicit def _deriveUnivEq_HNil: UnivEq[HNil] = UnivEq.force
  @inline implicit def _deriveUnivEq_CNil: UnivEq[CNil] = UnivEq.force

  @inline implicit def _deriveUnivEq_HCons[H, T <: HList](implicit eh: Lazy[UnivEq[H]], et: Lazy[UnivEq[T]]): UnivEq[H :: T] =
    UnivEq.force

  @inline implicit def _deriveUnivEq_CCons[H, T <: Coproduct](implicit eh: Lazy[UnivEq[H]], et: Lazy[UnivEq[T]]): UnivEq[H :+: T] =
    // Only inhabitants are Inl & Inr which are case classes
    UnivEq.force

  @inline def deriveUnivEq[F, G](implicit gen: Generic.Aux[F, G], e: Lazy[UnivEq[G]]): UnivEq[F] =
    UnivEq.force

  object AutoDerive extends TypeclassDerivation_LowPri_Auto {
    @inline implicit def deriveUnivEq[F, G](implicit gen: Generic.Aux[F, G], e: Lazy[UnivEq[G]]): UnivEq[F] =
      TypeclassDerivation.deriveUnivEq[F, G]
  }
}
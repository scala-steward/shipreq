package shipreq.webapp.base.vali2

import scalaz.Isomorphism.<=>
import scalaz.{-\/, Semigroup, \/, \/-}
import shipreq.base.util.{GenTuple, Identity}

object Generic {

  // ===================================================================================================================
  // Correction
  // ===================================================================================================================

  final case class EndoCorrector[A](live: A => A, full: A => A) {

    def appendLive(f: A => A): EndoCorrector[A] =
      EndoCorrector(live andThen f, full)

    def appendFull(f: A => A): EndoCorrector[A] =
      EndoCorrector(live, full andThen f)

    def append(that: EndoCorrector[A]): EndoCorrector[A] =
      EndoCorrector(
        live andThen that.live,
        full andThen that.full)

    def prependLive(f: A => A): EndoCorrector[A] =
      EndoCorrector(live compose f, full)

    def prependFull(f: A => A): EndoCorrector[A] =
      EndoCorrector(live, full compose f)

    def prepend(that: EndoCorrector[A]): EndoCorrector[A] =
      EndoCorrector(
        live compose that.live,
        full compose that.full)

    def xmap[B](g: A => B)(f: B => A): EndoCorrector[B] =
      EndoCorrector(
        g compose live compose f,
        g compose full compose f)

    def imap[B](iso: B <=> A): EndoCorrector[B] =
      xmap(iso.from)(iso.to)

    def tuple[A2, AA](b: EndoCorrector[A2])(implicit A: GenTuple[A, A2, AA]): EndoCorrector[AA] =
      EndoCorrector[AA](
        aa => A.map(aa, live, b.live, A.append),
        aa => A.map(aa, full, b.full, A.append))

    def toCorrector: Corrector[A, A] =
      Corrector(live, full, Identity[A])

    def toEndoValidator[E]: EndoValidator[E, A] =
      EndoValidator(this, Invalidator.id)

    def /[E](i: Invalidator[E, A]): EndoValidator[E, A] =
      EndoValidator(this, i)
  }

  object EndoCorrector {
    def id[A]: EndoCorrector[A] =
      EndoCorrector(Identity[A], Identity[A])

    def live[A](f: A => A): EndoCorrector[A] =
      EndoCorrector(f, Identity[A])

    def full[A](f: A => A): EndoCorrector[A] =
      EndoCorrector(Identity[A], f)
  }

  // ===================================================================================================================

  final case class Corrector[I, C](live     : I => I,
                                   full     : I => C,
                                   uncorrect: C => I) {

    def fullAndBack(i: I): I =
      uncorrect(full(i))

    def xmapInput[A](g: I => A)(f: A => I): Corrector[A, C] =
      Corrector(
        g compose live compose f,
        full compose f,
        g compose uncorrect)

    def imapInput[A](iso: A <=> I): Corrector[A, C] =
      xmapInput(iso.from)(iso.to)

    def xmapCorrected[A](f: C => A)(g: A => C): Corrector[I, A] =
      Corrector(
        live,
        f compose full,
        uncorrect compose g)

    def imapCorrected[A](iso: C <=> A): Corrector[I, A] =
      xmapCorrected(iso.to)(iso.from)

    def tuple[I2, C2, II, CC](b: Corrector[I2, C2])(implicit I: GenTuple[I, I2, II], C: GenTuple[C, C2, CC]): Corrector[II, CC] =
      Corrector[II, CC](
        ii => I.map(ii, live, b.live, I.append),
        ii => I.map(ii, full, b.full, C.append),
        cc => C.map(cc, uncorrect, b.uncorrect, I.append))

//    def >=>[C2](that: Corrector[I, C2]): Corrector[I, C2] =
//      Corrector(
//        live andThen that.live,
//        full andThen uncorrect andThen that.full,
//        that.uncorrect)

    def /[E, V](v: Auditor[E, C, V]): Validator[E, I, C, V] =
      Validator(this, v)
  }

  object Corrector {
    def id[A]: Corrector[A, A] =
      Corrector(Identity[A], Identity[A], Identity[A])

    def full[I, C](full: I => C, uncorrect: C => I): Corrector[I, C] =
      Corrector(Identity[I], full, uncorrect)
  }

  // ===================================================================================================================
  // Validation
  // ===================================================================================================================

  final case class Invalidator[+E, A](invalidate: A => Option[E]) extends AnyVal {
    @inline def apply(a: A): Option[E] =
      invalidate(a)

    def contramap[B](f: B => A): Invalidator[E, B] =
      Invalidator(invalidate compose f)

    def mapError[F](f: E => F): Invalidator[F, A] =
      Invalidator(invalidate(_).map(f))

    def merge[EE >: E](that: Invalidator[EE, A])(implicit E: Semigroup[EE]): Invalidator[EE, A] =
      Invalidator(a =>
        (invalidate(a), that.invalidate(a)) match {
          case (None      , None      ) => None
          case (e@ Some(_), None      ) => e
          case (None      , e@ Some(_)) => e
          case (Some(e1)  , Some(e2)  ) => Some(E.append(e1, e2))
        }
      )

    def liftOption: Invalidator[E, Option[A]] =
      Invalidator(_ flatMap invalidate)

    def whenValid[EE >: E](next: Invalidator[EE, A]): Invalidator[EE, A] =
      Invalidator(a => invalidate(a) orElse next.invalidate(a))

    def toEndoValidator[EE >: E]: EndoValidator[EE, A] =
      EndoValidator(EndoCorrector.id, this)
  }

  object Invalidator {
    def id[A]: Invalidator[Nothing, A] =
      Invalidator(_ => None)
  }

  // ===================================================================================================================

  final case class Auditor[+E, C, V](audit: C => E \/ V) extends AnyVal {
    @inline def apply(c: C): E \/ V =
      audit(c)

    def contramap[A](f: A => C): Auditor[E, A, V] =
      Auditor(audit compose f)

    def mapError[F](f: E => F): Auditor[F, C, V] =
      Auditor(audit(_) leftMap f)

    def mapValid[A](f: V => A): Auditor[E, C, A] =
      Auditor(audit(_) map f)

    def appendInvalidator[EE >: E](i: Invalidator[EE, V]): Auditor[EE, C, V] =
      Auditor(c => audit(c).flatMap(v => i.invalidate(v) match {
        case None => \/-(v)
        case Some(e) => -\/(e)
      }))

    def tuple[EE >: E, C2, V2, CC, VV](b: Auditor[EE, C2, V2])(implicit E: Semigroup[EE], C: GenTuple[C, C2, CC], V: GenTuple[V, V2, VV]): Auditor[EE, CC, VV] =
      Auditor[EE, CC, VV](cc => {
        val (c1, c2) = C.init(cc)
        def v1 = audit(c1)
        def v2 = b.audit(c2)
        errorApply(v1, v2)(V.append)(E)
      })

    def liftOption: Auditor[E, Option[C], Option[V]] =
      Auditor({
        case None    => \/-(None)
        case Some(c) => audit(c).map(Some(_))
      })
  }

  object Auditor {
    def id[A]: Auditor[Nothing, A, A] =
      Auditor(\/-(_))
  }

  def errorApply[E, A, B, C](x: E \/ A, y: E \/ B)(f: (A, B) => C)(implicit E: Semigroup[E]): E \/ C =
    (x, y) match {
      case (\/-(a), \/-(b)) => \/-(f(a, b))
      case (\/-(_), e@ -\/(_)) => e
      case (e@ -\/(_), \/-(_)) => e
      case (-\/(e1), -\/(e2)) => -\/(E.append(e1, e2))
    }

  // ===================================================================================================================
  // Correction + Validation
  // ===================================================================================================================

  final case class EndoValidator[+E, A](corrector: EndoCorrector[A],
                                       invalidator: Invalidator[E, A]) {

    def append[EE >: E](that: EndoValidator[EE, A])(implicit E: Semigroup[EE]): EndoValidator[EE, A] =
      EndoValidator(
        corrector append that.corrector,
        invalidator merge that.invalidator)

    def prepend[EE >: E](that: EndoValidator[EE, A])(implicit E: Semigroup[EE]): EndoValidator[EE, A] =
      EndoValidator(
        corrector prepend that.corrector,
        invalidator merge that.invalidator)

    def appendCorrector(c: EndoCorrector[A]): EndoValidator[E, A] =
      EndoValidator(corrector append c, invalidator)

    def prependCorrector(c: EndoCorrector[A]): EndoValidator[E, A] =
      EndoValidator(corrector prepend c, invalidator)

    def mapInvalidator[F](f: Invalidator[E, A] => Invalidator[F, A]): EndoValidator[F, A] =
      EndoValidator(corrector, f(invalidator))
  }

  object EndoValidator {
    def id[A]: EndoValidator[Nothing, A] =
      EndoValidator(EndoCorrector.id, Invalidator.id)
  }

  // ===================================================================================================================

  final case class Validator[+E, I, C, V](corrector: Corrector[I, C], auditor: Auditor[E, C, V]) {
    def apply(i: I): E \/ V =
      auditor(corrector.full(i))

    def mapAuditor[EE, VV](f: Auditor[E, C, V] => Auditor[EE, C, VV]): Validator[EE, I, C, VV] =
      Validator(corrector, f(auditor))

    def mapError[F](f: E => F): Validator[F, I, C, V] =
      mapAuditor(_.mapError(f))

    def mapValid[A](f: V => A): Validator[E, I, C, A] =
      mapAuditor(_.mapValid(f))

    def appendInvalidator[EE >: E](i: Invalidator[EE, V]) =
      mapAuditor(_.appendInvalidator(i))

    def tuple[EE >: E, I2, C2, V2, II, CC, VV](that: Validator[EE, I2, C2, V2])(implicit E: Semigroup[EE], I: GenTuple[I, I2, II], C: GenTuple[C, C2, CC], V: GenTuple[V, V2, VV]): Validator[EE, II, CC, VV] =
      Validator(
        corrector tuple that.corrector,
        auditor tuple that.auditor)
  }

  object Validator {
    def id[A]: Validator[Nothing, A, A, A] =
      Validator(Corrector.id, Auditor.id)
  }
}

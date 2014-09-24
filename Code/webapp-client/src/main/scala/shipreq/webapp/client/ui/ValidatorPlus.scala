package shipreq.webapp.client.ui

import scalaz.{Failure, Success, Endo}
import shipreq.webapp.shared.validation._

class ValidatorPlus[I, C, V](val liveCorrect: I => I, cp: CorrectionPart[I, C], vp: ValidationPart[C, V])
  extends Validator[I, C, V](cp, vp) {

  def stateful[S](t: ValidateS[S, V]): S => ValidatorPlus[I, C, V] =
    s => {
      val vp2 = ValidationPart[C, V](c =>
        validate(c) match {
          case f@ Failure(_) => f
          case r@ Success(v) => t(s, v).fold(r: ValidationResult[V])(Failure(_))
        })
      new ValidatorPlus[I, C, V](liveCorrect, cp, vp2)
    }
}

object ValidatorPlus {

  def apply[I, C, V](cp: CorrectionPart[I, C], vp: ValidationPart[C, V], liveCorrect: Endo[I]): ValidatorPlus[I, C, V] =
    new ValidatorPlus(liveCorrect.run, cp, vp)

  def apply[I, C, V](cp: CorrectionPart[I, C], vp: ValidationPart[C, V], liveCorrect: I => I): ValidatorPlus[I, C, V] =
    new ValidatorPlus(liveCorrect, cp, vp)

  def nop[A] =
    apply(CorrectionPart.nop[A], ValidationPart.nop[A], Endo.idEndo[A])
}
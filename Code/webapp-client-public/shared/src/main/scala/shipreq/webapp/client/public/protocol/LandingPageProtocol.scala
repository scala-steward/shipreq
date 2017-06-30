package shipreq.webapp.client.public.protocol

import boopickle.Pickler
import shipreq.webapp.base.protocol._
import shipreq.webapp.base.user._
import shipreq.webapp.base.validation._
import shipreq.webapp.base.validation.Implicits._
import BoopickleMacros._
import BinCodecGeneric._
import BinCodecUser._

object LandingPageProtocol {

  final case class Request(name      : PersonName,
                           email     : EmailAddr,
                           msg       : Option[String],
                           newsletter: Boolean) {

    def toValidatorInput: Request.ValidatorInput =
      (name.value, email.value, msg getOrElse "", newsletter)
  }

  object Request {
    implicit val pickler: Pickler[Request] = pickleCaseClass[Request]

    def labelName  = "Your name"
    def labelEmail = "Your email address"

    def validatorName  = UserValidators.personName
    def validatorEmail = UserValidators.emailAddr.unnamed
    def validatorMsg   = CommonValidation.optionalLargeText

    type ValidatorInput = (String, String, String, Boolean)

    lazy val validator: Composite.Validator[ValidatorInput, _, Request] =
      validatorName.named(labelName).named
        .tuple(validatorEmail.named(labelEmail).named)
        .tuple(validatorMsg.named("Your message").named)
        .tuple(Composite.Validator.id[Boolean])
        .mapValid((Request.apply _).tupled)
  }

  val Fn = ServerSideProc.Protocol[Request, Unit]
}

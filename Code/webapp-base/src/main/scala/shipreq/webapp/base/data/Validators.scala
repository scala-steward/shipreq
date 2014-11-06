package shipreq.webapp.base.data

import shipreq.webapp.base.AppConsts._
import shipreq.webapp.base.TextMod._
import shipreq.webapp.base.UiText.FieldNames
import shipreq.webapp.base.validation._
import Constraints._
import GenericValidators._
import ValidatorPlus.Implicits._

object Validators {

  object reqType {

    val mnemonic = {
      val validChars = WhitelistCharsR("A-Z", "may only consist of letters.")
      val validLength = LengthInRange(reqTypeMnemonicLength)
      ValidatorPlus(
        CorrectionPart.endo(noWhitespace andThen upperCase),
        ValidationPart.forConstraint("Mnemonic",
          nonEmpty >> (validChars.constraint + validLength))
          .map(ReqType.Mnemonic),
        upperCase andThen validChars andThen validLength)
    }

    val name = mandatoryShortText("Name").liftToPlus
  }

  object customIncmpType {
    def key = refKey
    def desc = optionalLargeText(FieldNames.desc).liftToPlus
  }

  val refKey = {
    // DD-18: Hashtag-like refkeys (groupings, incmp) must match this format: /[A-Za-z0-9][A-Za-z0-9_-=.]*/
    // Must not contain: []{}<>
    // TODO should uniqueness and matching be case-insensitive?
    val validChars = WhitelistCharsR("""A-Za-z0-9\._=\-""", "may only consist of letters, numbers, and these symbols: . _ = -")
    val validLength = LengthInRange(refKeyLength)
    ValidatorPlus(
      CorrectionPart.endo(noWhitespace),
      ValidationPart.forConstraint(FieldNames.refKey,
        nonEmpty >> (startsWithAlphaNumeric + validChars + validLength))
        .map(RefKey.apply),
      validChars andThen truncateToLength(refKeyLength))
  }
}
package shipreq.webapp.base.data

import scalaz.std.string.stringInstance
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.AppConsts._
import shipreq.webapp.base.TextMod._
import shipreq.webapp.base.UiText.FieldNames
import shipreq.webapp.base.data.ReqType.Mnemonic
import shipreq.webapp.base.validation._
import Constraints._
import GenericValidators._

object Validators {

  object reqType {

    val mnemonicU =
      Rules.whitelistCharsR("A-Z", "may only consist of letters.")
        .addRule(Rules.lengthInRange(reqTypeMnemonicLength))
        .liveCorrect(upperCase.andThen)
        .correct(_ andThen noWhitespace andThen upperCase)
        .constraint(nonEmpty >> _)
        .forField("Mnemonic")
        .map(ReqType.Mnemonic)

    val nameU = mandatoryShortText("Name")

    type S = (Stream[CustomReqType], Option[CustomReqType.Id])

    private def mnemonicUniqueness = {
      val static = (none[CustomReqType.Id],  ReqType.staticMnemonics)
      Uniqueness.againstSetByKeyO[S, CustomReqType.Id, Mnemonic](
        sr => sr._2,
        sr => static #:: sr._1.map(_.tmap2(_.id.some, _.allMnemonics))
      ).fieldName(FieldNames.mnemonic)
    }

    val mnemonicS = mnemonicU.liftS[S].addValidation(mnemonicUniqueness)

    private def nameUniqueness =
      Uniqueness.entity[CustomReqType].applyO(_.id.some, _.name).fieldName("Name")

    val nameS = nameU.liftS[S].addValidation(nameUniqueness)

    val all = mnemonicS ⊗ nameS ⊗ ValidatorU.nop[ImplicationRequired].liftS[S]
  }

  object customIncmpType {
    def key = refKey
    def desc = optionalLargeText(FieldNames.desc)
  }

  // DD-18: Hashtag-like refkeys (groupings, incmp) must match this format: /[A-Za-z0-9][A-Za-z0-9_-=.]*/
  // Must not contain: []{}<>
  // TODO should uniqueness and matching be case-insensitive?
  val refKey =
    Rules.whitelistCharsR( """A-Za-z0-9\._=\-""", "may only consist of letters, numbers, and these symbols: . _ = -")
      .addRule(Rules.lengthInRange(refKeyLength))
      .correct(noWhitespace.compose)
      .constraint(c => nonEmpty >> (startsWithAlphaNumeric + c))
      .forField(FieldNames.refKey)
      .map(RefKey.apply)
}
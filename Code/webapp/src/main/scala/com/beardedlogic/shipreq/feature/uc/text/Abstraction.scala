package com.beardedlogic.shipreq.feature.uc.text

import com.beardedlogic.shipreq.lib.Types._
import com.beardedlogic.shipreq.feature.uc.change._
import com.beardedlogic.shipreq.feature.uc.UcParsingCtx

trait ParsedText {
  val text: String
  @inline final def isEmpty = text.isEmpty
  @inline final def nonEmpty = text.nonEmpty
  def normalisedText(implicit savedSteps: SavedSteps): NormalisedText
}

object ParsedTextUpdater {
  @inline final def performReplacementsOnUpdate(z: String @@ InputCorrected): String @@ InputCorrected =
    ((z: String) /: ParsingConfig.PreprocessReplacements)((t, r) => r(t)).tag
}

trait ParsedTextUpdater[T <: ParsedText] {
  def correctInput(input: String): String @@ InputCorrected

  final def update(t: T, input: String)(implicit ctx: UcParsingCtx): ChangeResult[T, Change] =
    updateCorrected(t, correctInput(input))

  final def updateCorrected(t: T, newText: String @@ InputCorrected)(implicit ctx: UcParsingCtx): ChangeResult[T, Change] = {

    // TODO This would make more in parse() but is here so that it isn't applied when parsing loaded data (from DB)
    val newText2 = ParsedTextUpdater.performReplacementsOnUpdate(newText)

    if (t.text == newText2)
      NoChange
    else
      updateCorrected2(t, newText2)
  }

  protected def updateCorrected2(t: T, newText: String @@ InputCorrected)(implicit ctx: UcParsingCtx): ChangeResult[T, Change]

  final def updateAndGet(t: T, input: String)(implicit ctx: UcParsingCtx): T = update(t, input).getValueOrElse(t)
  final def updateCorrectedAndGet(t: T, input: String @@ InputCorrected)(implicit ctx: UcParsingCtx): T = updateCorrected(t, input).getValueOrElse(t)

}
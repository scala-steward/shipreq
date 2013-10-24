package com.beardedlogic.usecase.feature.uc.text

import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.feature.uc.change._
import com.beardedlogic.usecase.feature.uc.UcParsingCtx

trait ParsedText {
  val text: String
  @inline final def isEmpty = text.isEmpty
  @inline final def nonEmpty = text.nonEmpty
  def normalisedText(implicit savedSteps: SavedSteps): NormalisedText
}

trait ParsedTextUpdater[T <: ParsedText] {
  def correctInput(input: String): String @@ InputCorrected
  def updateCorrected(t: T, newText: String @@ InputCorrected)(implicit ctx: UcParsingCtx): ChangeResult[T, Change]

  final def update(t: T, input: String)(implicit ctx: UcParsingCtx): ChangeResult[T, Change] = {
    val newText = correctInput(input)
    if (t.text == newText)
      NoChange
    else
      updateCorrected(t, newText)
  }

  final def updateAndGet(t: T, input: String)(implicit ctx: UcParsingCtx): T = update(t, input).getValueOrElse(t)
  final def updateCorrectedAndGet(t: T, input: String @@ InputCorrected)(implicit ctx: UcParsingCtx): T = updateCorrected(t, input).getValueOrElse(t)
}
package com.beardedlogic.usecase.feature.uc.text

import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.feature.uc.change._
import com.beardedlogic.usecase.feature.uc.UcParsingCtx

trait Parser[T <: ParsedText[T]] {

  def empty: T

  def load(text: NormalisedText)(implicit savedSteps: SavedSteps, ctx: UcParsingCtx): T

  def parse(text: String)(implicit ctx: UcParsingCtx): T
}

trait ParsedText[Self <: ParsedText[Self]] extends ChangeResponder[Self] {
  this: Self =>

  val text: String

  @inline final def isEmpty = text.isEmpty
  @inline final def nonEmpty = text.nonEmpty

  def normalisedText(implicit savedSteps: SavedSteps): NormalisedText

  /** Does this text contain any step references? */
  def hasRefs_? : Boolean

  def update(input: String)(implicit ctx: UcParsingCtx): ChangeResult[Self, Change] = {
    val newText = correctInput(input)
    if (text == newText) NoChange
    else updateCorrected(newText)
  }

  protected def correctInput(input: String): String @@ InputCorrected

  protected def updateCorrected(newText: String @@ InputCorrected)(implicit ctx: UcParsingCtx): ChangeResult[Self, Change]
}

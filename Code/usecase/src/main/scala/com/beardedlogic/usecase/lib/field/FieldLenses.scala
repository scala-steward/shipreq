package com.beardedlogic.usecase
package lib.field

import scalaz.Lens.{lensg, lensFamilyg}
import lib.Types._
import lib.text.{StepText, FreeText}
import lib.UseCase
import lib.UseCaseHeader
import util.LensFns._

// TODO move and rename "Field" lenses. Maybe UC lenses
object FieldLenses {

  // Header lenses
  object hdr {
    val titleL = lensg[UseCaseHeader, String](h => t => h.copy(title = t), _.title)
    val numberL = lensg[UseCaseHeader, Short](h => n => h.copy(number = n), _.number)
  }

  // Step field lenses
  object sfv {
    val stepText = KeyedLens[StepFieldValue, LocalStepId, StepText](
      sfv => id => newValue => sfv.copy(textmap = sfv.textmap + (id -> newValue)),
      sfv => id => sfv.textmap(id)
    )
  }

  val freeText = lensFamilyg[FreeText, FreeText, String, (String, StepAndLabelBiMap)](
    _ => input => FreeText.parse(input._1)(input._2),
    _.text)

  val stepText = lensFamilyg[StepText, StepText, String, (String, StepAndLabelBiMap)](
    v => input => StepText.parse(v.stepId, input._1)(input._2),
    _.text)

  // Use case lenses
  object uc {

    val header = lensg[UseCase, UseCaseHeader](u => h => u.copy(h), _.header)

    val textField = KeyedLens[UseCase, TextField, FreeText](
      uc => f => v => uc.copy(fieldValues = uc.fieldValues + (f ~> v)),
      uc => f => f(uc.fieldValues)
    )

    val stepField = KeyedLens[UseCase, StepField, StepFieldValue](
      uc => f => v => uc.copy(fieldValues = uc.fieldValues + (f ~> v)),
      uc => f => f(uc.fieldValues)
    )

    val title = header >=> hdr.titleL
    val number = header >=> hdr.numberL
    val stepText = stepField >@=@> sfv.stepText
  }
}

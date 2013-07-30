package com.beardedlogic.usecase.lib.field

import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.lib.text.FreeText
import com.beardedlogic.usecase.model._
import com.beardedlogic.usecase.lib.UseCase

// =====================================================================================================================

case class TextFieldDefinition(title: String) extends FieldDefinition {
  override val fieldKeyType = FieldKeyType.Text
  override val fieldKeyData = Some(title)
  override def field(rec: FieldKeyRec) = TextField(this, rec)
}

// =====================================================================================================================

case class TextField(override val defn: TextFieldDefinition, override val rec: FieldKeyRec) extends Field {
  override type Value = FreeText
  override type State = TextWithNormalisedRefs

  override def empty = FreeText.empty

  override def load(loadCtx: FieldLoadCtx, mutableSaveCtx: MutableFieldSaveCtx) =
    loadCtx.fieldValues.get(rec).flatMap(_.fieldData).getOrElse("").hasNormalisedRefs

  override def denormalise(normalisedState: TextWithNormalisedRefs, savedSteps: SavedSteps) =
    (None, (stepsAndLabels: StepAndLabelBiMap) => FreeText.load(normalisedState)(savedSteps, stepsAndLabels))

  override def valueSaver(v: FreeText) = new TextFieldValueSaver(v)

  def updateText(newText: String)(uc: UseCase): UcUpdateResult = {
    implicit val lens = alens(FieldLenses.uc.textField, (uc, this))
    uc.update(this, lens.get.update(newText)(uc.stepsAndLabels))
  }
}

// =====================================================================================================================

class TextFieldValueSaver(val v: FreeText) extends FieldValueSaver[TextWithNormalisedRefs] {
  type S = TextWithNormalisedRefs

  def state(savedSteps: SavedSteps): S = v.textWithNormalisedRefs(savedSteps)

  override def record_required_? = v.text.nonEmpty

  override def presave(dao: DAO, prevSave: Option[(FieldSaveCtx, S)], savedSteps: SavedSteps)(saveCtx: MutableFieldSaveCtx) = {
    prevSave match {
      case None => true
      case Some((_, previousText)) => previousText != state(savedSteps)
    }
  }

  override def save(dao: DAO, savedSteps: SavedSteps, combinedSaveCtx: FieldSaveCtx, newSaveCtx: FieldSaveCtx) = {
    // Required again because normalised refs may be different after presave
    val ntxt = state(savedSteps)
    (Some(ntxt), ntxt)
  }
}

package shipreq.webapp.server.feature.uc.field

import shipreq.webapp.server.db.{FieldKeyType, FieldKeyRec}
import shipreq.webapp.server.feature.uc.Lenses
import shipreq.webapp.server.feature.uc.change.Changes.TextChanged
import shipreq.webapp.server.feature.uc.change.{UcUpdateResult, ChangeResult, UseCaseUpdater}
import shipreq.webapp.server.feature.uc.text.{FreeTextUpdater, FreeText}
import shipreq.webapp.server.feature.validation.Validators
import shipreq.webapp.server.util.AppliedLens

// =====================================================================================================================

case class TextFieldDefinition(title: String) extends FieldDefinition {
  override val fieldKeyType = FieldKeyType.Text
  override val fieldKeyData = Some(title)
  override def field(rec: FieldKeyRec) = TextField(this, rec)
}

// =====================================================================================================================

trait TextFieldLike { this: Field with TextField =>
  override type Value = FreeText

  override def empty = FreeText.empty

  override def toString = s"${getClass.getSimpleName}[#${rec.id.value}:${defn.title}]"

  override val changeResponder = new FreeTextUpdater(TextChanged(this))

  def updateText(newText: String)(u: UseCaseUpdater): UcUpdateResult =
    ChangeResult.fromValidationIC(Validators.usecase.textFieldText correctAndValidateU newText)(t => {
      implicit val lens = AppliedLens(Lenses.ucTextFieldL, (u.uc, this))
      val cr = changeResponder.updateCorrected(lens.get, t)(u.ctx)
      u.update(cr)
    })
}

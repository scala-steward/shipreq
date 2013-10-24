package com.beardedlogic.usecase
package feature.uc
package field

import db.{FieldKeyType, FieldKeyRec}
import lib.Types._
import change.UseCaseUpdater
import change.Changes.TextChanged
import text.{FreeTextUpdater, FreeText}

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

  override def toString = s"${getClass.getSimpleName}[#${rec.id}:${defn.title}]"

  override val changeResponder = new FreeTextUpdater(TextChanged(this))

  def updateText(newText: String)(u: UseCaseUpdater): UcUpdateResult = {
    implicit val lens = alens(Lenses.ucTextFieldL, (u.uc, this))
    val cr = changeResponder.update(lens.get, newText)(u.ctx)
    u.update(this, cr)
  }
}

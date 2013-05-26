package com.beardedlogic.usecase
package model

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import lib.UCEditorState

case class UseCase(
  valueId: Long,
  title: String,
  number: Short,
  fieldList: FieldList) extends Value[DataType.UseCase]

object UseCaseAccessor {
  val Insert = Q.update[(Long, String, Short, Long)]("INSERT INTO usecase VALUES(?,?,?,?)")
}

trait UseCaseAccessor extends DatabaseAccessor {
  self: ValueAccessor with RelationAccessor with FieldValueAccessor =>

  import UseCaseAccessor._

  def createInitialUseCase(uc: UCEditorState): UseCase = db.withTransaction {
    // Save UC
    val ucValue = createInitialValue(DataType.UseCase)
    Insert.execute(ucValue.valueId, uc.title, uc.ucId.toShort, uc.fieldList.valueId)

    // Save and link to fields
    for (fv <- createInitialFieldValue(uc.fields)) {
      relate_usecase_has_fieldValue(ucValue, fv)
    }

    UseCase(ucValue.valueId, uc.title, uc.ucId.toShort, uc.fieldList)
  }
}

package com.beardedlogic.usecase
package model

import lib.field._
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{StaticQuery => Q}

/**
 * Corresponds to the data type [[com.beardedlogic.usecase.model.DataType.FieldList]], which basically boils down to a
 * single `List[FieldDef]`s.
 */

object FieldList {

  def save(fieldList: List[FieldDef])(implicit s: Session): Value[DataType.FieldList.type] = s.withTransaction {
    val fieldListRecord = Value.createWithNewData(DataType.FieldList)
    var index = 0
    for (f <- fieldList) {
      val tf = f.asInstanceOf[TextFieldDef]
      val fieldKey = FieldKey.createWithNewData(FieldKeyType.Text, Some(tf.title)) // TODO hardcoded for text
      Relation.create(fieldListRecord, RelationType.Has, index.toShort, fieldKey.value)
      index += 1
    }
    fieldListRecord
  }

  def load(fieldListId: Long)(implicit s: Session): List[FieldDef] = {
    FieldKey.selectByFieldList(fieldListId).map { case (_, data) => TextFieldDef(data.get, None) }
  }
}

package com.beardedlogic.usecase
package model

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import lib.db._
import lib.Types._
import DBHelpers._

// TODO shouldn't this be typed by FieldKeyType?
case class FieldKeyRec(valueId: Long, fieldKeyType: FieldKeyType, fieldKeyData: FieldKeyRecData)
  extends Value[DataType.FieldKey] {

  def fieldDefn = fieldKeyType.fieldDefn(fieldKeyData)
  def field = fieldDefn.field(this)
  def taggedId = tag[FieldKeyId](valueId)
}

// ---------------------------------------------------------------------------------------------------------------------

object FieldKeyAccessor {

  implicit val GetResultFieldKey = GetResult { r => FieldKeyRec(r.<<, r.<<, r.<<) }

  val SelectIdToReuse = Q.query[(Short, FieldKeyRecData), Long](
    "SELECT id FROM field_key WHERE type_id=? AND data IS NOT DISTINCT FROM ?")

  val Insert = Q.update[(Long, Short, FieldKeyRecData)](
    "INSERT INTO field_key(id, type_id, data) VALUES(?,?,?)")

  val SelectByFieldList = Q.query[(Long, Short), FieldKeyRec]( """
      SELECT fk.id, fk.type_id, fk.data
      FROM field_key fk, relation r
      WHERE fk.id = r.to_id
        AND r.from_id = ?
        AND r.type_id = ?
      ORDER BY r.index """.sql)
}

trait FieldKeyAccessor extends DatabaseAccessor {
  self: ValueAccessor =>

  import FieldKeyAccessor._

  def findOrCreateInitialFieldKey(fieldKeyType: FieldKeyType, fieldKeyData: FieldKeyRecData) = db.withTransaction {
    SelectIdToReuse.firstOption(fieldKeyType, fieldKeyData)
    .map(FieldKeyRec(_, fieldKeyType, fieldKeyData))
    .getOrElse(createInitialFieldKey(fieldKeyType, fieldKeyData))
  }

  def createInitialFieldKey(fieldKeyType: FieldKeyType, fieldKeyData: FieldKeyRecData) = {
    val fkv = createInitialValue(DataType.FieldKey)
    Insert.first(fkv.valueId, fieldKeyType, fieldKeyData)
    FieldKeyRec(fkv.valueId, fieldKeyType, fieldKeyData)
  }

  def findAllFieldKeysByFieldList(fieldList: Value[DataType.FieldList]): List[FieldKeyRec] =
    SelectByFieldList.list(fieldList.valueId, RelationType.Has)
}
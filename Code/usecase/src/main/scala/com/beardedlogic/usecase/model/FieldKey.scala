package com.beardedlogic.usecase
package model

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import lib.db._
import DBHelpers._

case class FieldKey(value: PlainValue[DataType.FieldKey.type], fieldKeyType: FieldKeyType, data: Option[String])
  extends ValueExt[DataType.FieldKey.type]

object FieldKey extends DBTable {
  override val TableName = "field_key"

  val CreateWithNewData = Q.update[(Long, Short, Option[String])](s"INSERT INTO $TableName(id, type_id, data) VALUES(?,?,?)")

  def createWithNewData(fieldKeyType: FieldKeyType, data: Option[String])(implicit s: Session) = {
    val value = Value.createWithNewData(DataType.FieldKey)
    val newId = CreateWithNewData.first(value.id, fieldKeyType, data)
    FieldKey(value, fieldKeyType, data)
  }

  val SelectByFieldList = Q.query[(Long, Short), (FieldKeyType, Option[String])]( """
      SELECT fk.type_id, fk.data
      FROM field_key fk, relation r
      WHERE fk.id = r.to_id
        AND r.from_id = ?
        AND r.type_id = ?
      ORDER BY r.index """.sql)

  def selectByFieldList(fieldListId: Long)(implicit s: Session): List[(FieldKeyType, Option[String])] =
    SelectByFieldList.list(fieldListId, RelationType.Has)
}


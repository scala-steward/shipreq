package com.beardedlogic.usecase.model

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{StaticQuery => Q}
import com.beardedlogic.usecase.lib.db.DatabaseEnum
import com.beardedlogic.usecase.lib.EnumValue
import Q.interpolation

object BullshitImplicits {

  @inline implicit def enumToShort(enum: EnumValue): Short = enum.ordinal
  @inline implicit def shortToDataType(ordinal:Short): DataType = DataType(ordinal)
  @inline implicit def shortToRelationType(ordinal:Short): RelationType = RelationType(ordinal)
  @inline implicit def shortToFieldKeyType(ordinal:Short): FieldKeyType = FieldKeyType(ordinal)
}

import BullshitImplicits._

trait BullshitTable {
  val TableName: String

  def count(implicit s: Session) = Q.queryNA[Int](s"SELECT COUNT(1) FROM $TableName").first
}

object Data extends BullshitTable  {
  override val TableName = "data"

  def create[T <: DataType](dataType: T)(implicit s: Session): Data[T] = {
    val id = Q.query[Short, Int](s"INSERT INTO $TableName(type_id) VALUES(?) RETURNING id").first(dataType)
    Data(id, dataType)
  }

  def find(id: Long)(implicit s: Session): Data[_ <: DataType] = {
    val typeId = Q.query[Long, Short](s"SELECT type_id FROM $TableName WHERE id=?").first(id)
    Data(id, typeId)
  }
}

case class Data[T <: DataType](
  id: Long,
  dataType: T)

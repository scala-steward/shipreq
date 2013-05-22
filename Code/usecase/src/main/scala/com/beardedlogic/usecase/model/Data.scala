package com.beardedlogic.usecase
package model

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{StaticQuery => Q}
import lib.db._
import DBHelpers._

object Data extends DBTable {
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

case class Data[T <: DataType](id: Long, dataType: T)

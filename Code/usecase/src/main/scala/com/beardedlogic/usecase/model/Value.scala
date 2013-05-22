package com.beardedlogic.usecase
package model

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.{StaticQuery => Q}
import lib.db._
import DBHelpers._

object Value extends DBTable {
  override val TableName = "value"

  def createWithNewData[T <: DataType](dataType: T)(implicit s: Session): PlainValue[T] =
    create(Data.create(dataType), 1)

  def create[T <: DataType](data: Data[T], rev: Int)(implicit s: Session): PlainValue[T] = {
    val newId = Q.query[(Long, Int), Long](s"INSERT INTO $TableName(data_id, rev) VALUES(?,?) RETURNING id")
                .first(data.id, rev)
    PlainValue(newId, data.id, rev)
  }
}

trait Value[T <: DataType] {
  def id: Long
  def dataId: Long
  def rev: Int
}

trait ValueExt[T <: DataType] extends Value[T] {
  val value: Value[T]
  @inline def id = value.id
  @inline def dataId = value.dataId
  @inline def rev = value.rev
}

case class PlainValue[T <: DataType](id: Long, dataId: Long, rev: Int) extends Value[T]

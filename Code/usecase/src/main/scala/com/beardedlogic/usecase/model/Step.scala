package com.beardedlogic.usecase
package model

import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import lib.TypeTags._

object StepAccessor {

  val Insert = Q.update[(Long, String)]("INSERT INTO step VALUES(?,?)")

  implicit val GetResultStepValueId = GetResult(r => tag[StepId](r.nextLong))
}

trait StepAccessor extends DatabaseAccessor {
  import StepAccessor._

  def createStep(value: Value[DataType.Step], text: String): Unit = {
    Insert.execute(value.valueId, text)
  }

  def mapStepTextById(sqlCond: String): Map[Long_StepId, String] = {
    val map = Map.newBuilder[Long_StepId, String]
    Q.queryNA[(Long_StepId, String)]("SELECT id,text FROM step " + sqlCond).foreach(map += _)
    map.result
  }
}

/**
 * Marks a `DataType` as being allow to link to a `Step` with a `Has` relationship.
 */
trait StepParent {
  self: DataType =>
}
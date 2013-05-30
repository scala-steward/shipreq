package com.beardedlogic.usecase
package model

import scala.collection.mutable.{Map => MutableMap}
import scala.slick.jdbc.{GetResult, SetParameter, StaticQuery => Q}
import scala.slick.session.PositionedParameters
import lib.db.DBHelpers._
import lib.field.Field
import FieldValue.FieldValueData
import lib.TypeTags._

case class FieldValue(
  valueId: Long,
  fieldKeyId: Long,
  fieldData: FieldValueData
  ) extends Value[DataType.FieldValue] {

  def fieldKeyIdTagged = tag[FieldKeyId](fieldKeyId)
}

object FieldValue {
  type FieldValueData = Option[String]
}

// ---------------------------------------------------------------------------------------------------------------------

class FieldSaveCtx(val db: DAO) {

  /**
   * `Value` instances for all fields that will be saved in the current transaction.
   */
  val fieldValues = MutableMap.empty[Field[_], Value[DataType.FieldValue]]

  /**
   * Key is the step node ID.
   */
  val stepValues = MutableMap.empty[String, Value[DataType.Step]]
}

// ---------------------------------------------------------------------------------------------------------------------

class FieldLoadCtx(

  /**
   * A map of field key IDs to field values.
   */
  val fieldValues: Map[Long_FieldKeyId, FieldValue],

  /**
   * For each relation type, a map of from-IDs to to-IDs (in the order specified in the `index` column).
   */
  val relations: Map[RelationType, Map[Long, List[Long]]],

  /**
   * A map of step IDs to step `text`.
   */
  val stepData: Map[Long_StepId, String]
  )

// ---------------------------------------------------------------------------------------------------------------------

object FieldValueAccessor {

  implicit val GetResultFieldValue = GetResult(r => FieldValue(r.<<, r.<<, r.<<))

  implicit object SetParameterFieldValue extends SetParameter[FieldValue] {
    def apply(v: FieldValue, pp: PositionedParameters) {
      pp.setLong(v.valueId)
      pp.setLong(v.fieldKeyId)
      pp.setStringOption(v.fieldData)
    }
  }

  val Insert = Q.update[FieldValue]("INSERT INTO field_value VALUES(?,?,?)")

  val SelectByOwner = Q.query[Long, FieldValue]( s"""
      select fv.id, fv.field_key_id, fv.text
      from field_value fv, relation r
      where fv.id = r.to_id
      and r.type_id = 200
      and r.from_id = ?
      """.sql)
}

trait FieldValueAccessor extends DatabaseAccessor {
  self: DAO =>

  import FieldValueAccessor._

  def createFieldValue(value: Value[DataType.FieldValue], fieldKey: FieldKey, data: FieldValueData) = {
    val fv = FieldValue(value.valueId, fieldKey.valueId, data)
    Insert.execute(fv)
    fv
  }

  def getFieldLoadCtxFor(ownerId: Long): FieldLoadCtx = {

    // Load field values
    var fieldValues = Map.empty[Long_FieldKeyId, FieldValue]
    SelectByOwner.foreach(ownerId, { fv =>
      fieldValues += (fv.fieldKeyIdTagged -> fv)
    })

    // Load relations and extended field value data (ie. steps)
    val (hasRelations, stepData) =
      withTmpTableOfRecursiveHasRelations(ownerId, { case (tmpTable, hasRelations) =>
        val stepData = mapStepTextById(s"WHERE id IN (SELECT to_id FROM $tmpTable)")
        (hasRelations, stepData)
      })
    val relations = Map((RelationType.Has: RelationType) -> hasRelations)

    // Bundle
    new FieldLoadCtx(fieldValues, relations, stepData)
  }
}
package com.beardedlogic.usecase
package model

import lib.field.FieldDefinition

/**
 * Corresponds to the data type [[com.beardedlogic.usecase.model.DataType.FieldList]], which basically boils down to a
 * list of `FieldDefinition`s.
 */
case class FieldListRec(value: PlainValue[DataType.FieldList], fieldKeys: List[FieldKeyRec])
  extends Value[DataType.FieldList] {
  def valueId = value.valueId
  def data = Data(dataId, DataType.FieldList)
  def dataId = value.dataId

  val fields = fieldKeys.map(_.field)
  def fieldDefns = fieldKeys.map(_.fieldDefn)
}

trait FieldListAccessor extends DatabaseAccessor {
  self:DataAccessor with ValueAccessor with RelationAccessor with FieldKeyAccessor =>

  def createInitialFieldList(fields: List[FieldDefinition], idOpt: Option[Long] = None) = db.withTransaction {
    val data = createData(DataType.FieldList, idOpt)
    createFieldList(data, fields, ExactRev(1))
  }

  def createFieldList(data: Data[DataType.FieldList], fields: List[FieldDefinition], rev: Revision = LatestRev): FieldListRec = db.withTransaction {
    val value = createValue(data, rev)

    var fieldKeys = List.empty[FieldKeyRec]
    var index = 0
    for (f <- fields) {
      val fieldKey = findOrCreateInitialFieldKey(f.fieldKeyType, f.fieldKeyData)
      relate_fieldList_has_fieldKey(value, index.toShort, fieldKey)
      fieldKeys :+= fieldKey
      index += 1
    }

    FieldListRec(value, fieldKeys)
  }

  def findFieldList(data: Data[DataType.FieldList], rev: Revision): Option[FieldListRec] = {
    findValue(data, rev).map { value =>
      val fieldKeys = findAllFieldKeysByFieldList(value)
      FieldListRec(value, fieldKeys)
    }
  }

  /**
   * Ensures that a data & value exist in the DB that matches the given field list, and that it is the latest revision.
   *
   * @param id The data ID for the field list.
   * @param fields The field list to save.
   */
  def syncFieldList(id: Long, fields: List[FieldDefinition]): FieldListRec = db.withTransaction {
    val dataOp = findData(id, DataType.FieldList)
    val latestOp = dataOp.flatMap(findFieldList(_, LatestRev))

    (dataOp, latestOp) match {
      case (None, _)                                        => createInitialFieldList(fields, Some(id))
      case (_, Some(latest)) if latest.fieldDefns == fields => latest
      case (Some(data), _)                                  => createFieldList(data, fields)
    }
  }
}

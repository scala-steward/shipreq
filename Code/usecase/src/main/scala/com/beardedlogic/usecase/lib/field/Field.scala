package com.beardedlogic.usecase
package lib
package field

import scala.xml.NodeSeq
import model.{FieldLoadCtx, FieldKey, FieldKeyType}
import FieldKey.FieldKeyData

/**
 * @tparam S Field State type.
 */
trait FieldDef[S] {

  def newFieldInstance(ucCtx: UseCaseCtx, fieldKey: FieldKey): Field[S]

  def fieldKeyType: FieldKeyType

  /**
   * The arbitrary data stored in the database that comprises this field key's state.
   */
  def fieldKeyData: FieldKeyData

  def stateLoader(fieldKey: FieldKey): FieldStateLoader[S]
}

/**
 * Stateful instance of a Use Case Editor field (or fields).
 *
 * @tparam S Field State type.
 */
trait Field[S] {

  val ucCtx: UseCaseCtx

  val fieldKey: FieldKey

  @inline final def msgCentre = ucCtx.msgCentre

  /**
   * Called once after all fields have been created. Invocation is synchronous and must complete before the first
   * render is performed.
   */
  def init(): Unit

  def render(): NodeSeq
}
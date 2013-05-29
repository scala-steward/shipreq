package com.beardedlogic.usecase
package lib.field

import model.{FieldSaveCtx, FieldLoadCtx}
import model.FieldValue.FieldValueData

/**
 * Loads a field's state to the database.
 *
 * @tparam S Field state type.
 * @since 27/05/2013
 */
trait FieldStateLoader[S] {

  /**
   * Sets this object's state to a previously saved state, as provided by the load context.
   *
   * @param ctx A big blob of data for all fields, from which this field should find and use its own data.
   */
  def load(ctx: FieldLoadCtx): S
}

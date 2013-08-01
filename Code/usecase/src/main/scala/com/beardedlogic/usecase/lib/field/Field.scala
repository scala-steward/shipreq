package com.beardedlogic.usecase.lib.field

import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.model._
import com.beardedlogic.usecase.lib.{StepTree, UcChangeDomain}
import com.beardedlogic.usecase.lib.change.ChangeResponder

trait FieldDefinition {

  /** The type (enum) of this field. */
  val fieldKeyType: FieldKeyType

  /** Arbitrary data (to store in the database) that comprises this field key's state. */
  val fieldKeyData: FieldKeyRecData

  def field(rec: FieldKeyRec): Field
}

/**
 * Represents a field that a use case can have. Eg. "Frequency of Use", "Exception Courses"
 *
 * This does not include the value of the field.
 */
trait Field extends UcChangeDomain {

  /** The type of this field's values. */
  type Value <: ChangeResponder[Value]

  /** The type of this field's normalised state. */
  type State

  val defn: FieldDefinition

  /** The DB record used to reference this field. */
  val rec: FieldKeyRec

  @inline final def castValue(v: Field#Value) = v.asInstanceOf[Value]

  @inline final def castState(s: Field#State) = s.asInstanceOf[State]

  @inline final def apply(fieldValues: FieldValues): Value = castValue(fieldValues(this))

  @inline final def get(fieldValues: FieldValues): Option[Value] = fieldValues.get(this).asInstanceOf[Option[Value]]

  @inline final def ~>(fieldValue: Value): (Field, Field#Value) = this -> fieldValue

  @inline final def value(implicit fieldValues: FieldValues) = apply(fieldValues)

  def empty: Value

  /**
   * Builds a field value state a previously saved state, as provided by the load context.
   *
   * @param loadCtx A big blob of data for all fields, from which this field should find and use its own data.
   * @param mutableSaveCtx After loading, a load ctx is transformed into a save ctx so that it can be used as a save
   *                       checkpoint. Fields should update the saveCtx as required as they process the load ctx.
   */
  def load(loadCtx: FieldLoadCtx, mutableSaveCtx: MutableFieldSaveCtx): State

  def denormalise(s: State, savedSteps: SavedSteps): (Option[StepTree], StepAndLabelBiMap => Value)

  def valueSaver(v: Value): FieldValueSaver[State]
}

trait FieldValueSaver[S] {

  /**
   * Gives a field a chance to opt-out of storing a value in the database.
   * If a field is blank, then there's no point saving it.
   */
  def record_required_? : Boolean

  /**
   * Saves `data` and `value` rows for any additional data required.
   *
   * If a previous save is provided, the function should compare current and previous states to determine whether
   * anything needs to be saved.
   *
   * @return Whether the field's state has changed since the last save.
   */
  def presave(dao: DAO, prevSave: Option[(FieldSaveCtx, S)], savedSteps: SavedSteps)(saveCtx: MutableFieldSaveCtx): Boolean

  /**
   * Continues saving state to database.
   *
   * Because `presave()` is called on all fields before any fields reach this method, all `data` and `value` rows will
   * have been saved, the IDs known.
   *
   * @param combinedSaveCtx The save context of all new rows on top of the save context for the previous save. Rows
   *                        being reused will be found here.
   * @param newSaveCtx The save context for all new rows. Rows being reused will not be found here.
   * @return A single, arbitrary data string that will be stored in `field_value.data`. The format and mechanism of this
   *         value can be decided by the field type.
   */
  def save(dao: DAO, savedSteps: SavedSteps, combinedSaveCtx: FieldSaveCtx, newSaveCtx: FieldSaveCtx): (FieldValueRecData, S)
}

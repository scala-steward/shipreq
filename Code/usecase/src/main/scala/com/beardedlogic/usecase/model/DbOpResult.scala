package com.beardedlogic.usecase.model

/**
 * Indicates a type of result of a high-level database operation.
 *
 * @since 14/06/2013
 */
sealed trait DbOpResult {
  def isSuccess: Boolean
  @inline final def isFailure = !isSuccess
}
/** Database operation was a success. */
sealed trait DbOpSuccess extends DbOpResult {
  override def isSuccess = true
}
/** Database operation failed. */
sealed trait DbOpFailure extends DbOpResult {
  override def isSuccess = false
}

/** Container for DbOpResult constants. */
object DbOpResult {

  /** A new revision of a value was successfully created. */
  case object NewRevision extends DbOpSuccess

  /** A value was successfully updated directly. No new revision or audit trail was created. */
  case object DirectUpdate extends DbOpSuccess

  /** The database operation was determined to have no effect on the data. */
  case object AlreadyUpToDate extends DbOpSuccess

  /** The data used in the request is now out-of-date, therefore the operation was aborted. */
  case object StaleRevision extends DbOpFailure
}

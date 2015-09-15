package shipreq.webapp.base.event

import shipreq.base.util.{NonEmptyVector, UnivEq}

sealed abstract class DeletionAction
object DeletionAction {
  def values = NonEmptyVector[DeletionAction](Delete, Restore)
  @inline implicit def equality: UnivEq[DeletionAction] = UnivEq.force
}

/**
 * Mark data as being [[shipreq.webapp.base.data.Dead]].
 */
case object Delete extends DeletionAction

/**
 * Restore [[shipreq.webapp.base.data.Dead]] data back to [[shipreq.webapp.base.data.Live]].
 */
case object Restore extends DeletionAction

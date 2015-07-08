package shipreq.webapp.base.event

import shipreq.base.util.UnivEq

// TODO Delete DeletionAction from .protocol
sealed abstract class DeletionAction

object DeletionAction {

  /**
   * Permanently remove data.
   */
  case object HardDel extends DeletionAction

  /**
   * Mark data as being [[shipreq.webapp.base.data.Dead]].
   */
  case object SoftDel extends DeletionAction

  /**
   * Restore [[shipreq.webapp.base.data.Dead]] data back to [[shipreq.webapp.base.data.Live]].
   */
  case object Restore extends DeletionAction

  // def values = NonEmptyVector[DeletionAction](HardDel, SoftDel, Restore)
  @inline implicit def equality: UnivEq[DeletionAction] = UnivEq.force
}

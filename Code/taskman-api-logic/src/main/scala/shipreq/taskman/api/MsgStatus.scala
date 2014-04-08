package shipreq.taskman.api

sealed trait MsgStatus

object MsgStatus {
  sealed trait Live extends MsgStatus
  case object Unassigned extends Live
  case object NodeAssigned extends Live
  case object Working extends Live

  sealed trait Archived extends MsgStatus
  case object Complete extends Archived
  case object Aborted extends Archived
}
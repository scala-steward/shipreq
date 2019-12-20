package shipreq.taskman.server.logic

import java.time.{Duration, Instant}
import shipreq.base.util.ArticulateError
import shipreq.taskman.api.Priority

/** Server Operation.
  * An operation in the domain of taskman-server, rather than taskman-api or any business logic.
  */
sealed trait ServerOp[A]

/** Represents an operation to handle a failed job. */
sealed trait FailedJobReaction extends ServerOp[Unit]

object ServerOp {

  /** Loads a configuration value.
    *
    * @param key The config key.
    */
  final case class CfgGet(key: String) extends ServerOp[Option[String]]

  /** Assigns tasks to the given node id, and retrieves them.
    *
    * @param batchSize             The maximum number of msgs to assign and return.
    * @param assignmentTrustPeriod Period of time for which another node's assignment is respected.
    * @param queueStatus           The highest priority msg in, and size of the in-memory queue.
    */
  final case class GetTasksAssignNode(nodeId               : NodeId,
                                      batchSize            : Int,
                                      assignmentTrustPeriod: Duration,
                                      queueStatus          : Option[(Priority, Int)]) extends ServerOp[List[TaskHeader]]

  final case class GetTaskAssignWorker(nodeId  : NodeId,
                                       workerId: WorkerId,
                                       th      : TaskHeader) extends ServerOp[Option[TaskDetail]]

  /** Result = true if worker was successfully reassigned. */
  final case class ReassignWorker(nodeId  : NodeId,
                                  workerId: WorkerId,
                                  td      : TaskDetail) extends ServerOp[Boolean]

  final case class UpdateTaskSuccess(nodeId  : NodeId,
                                     workerId: WorkerId,
                                     td      : TaskDetail) extends ServerOp[Unit]

  final case class UpdateTaskRetry(nodeId  : NodeId,
                                   workerId: WorkerId,
                                   td      : TaskDetail,
                                   delay   : Duration) extends FailedJobReaction

  final case class UpdateTaskAbort(nodeId  : NodeId,
                                   workerId: WorkerId,
                                   td      : TaskDetail) extends FailedJobReaction

  final case class NotifySupportWorkerFailed(when: Instant,
                                             td  : TaskDetail,
                                             err : ArticulateError) extends ServerOp[Unit]

  final case class NotifySupportTaskmanError(when: Instant,
                                             err : ArticulateError,
                                             td  : Option[TaskDetail]) extends ServerOp[Unit]

  case object Nop extends ServerOp[Unit]

}

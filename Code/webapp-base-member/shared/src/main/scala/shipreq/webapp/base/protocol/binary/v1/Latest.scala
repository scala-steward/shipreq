package shipreq.webapp.base.protocol.binary.v1

/** This is a convenience for usages that don't need to care about versioning (eg. benchmarks, WW, tests).
  * It reduces the amount of busy-work required when bumping versions by allowing you to modify just this one
  * object rather than all uses that don't care and just need the latest.
  */
object Latest {

  @inline implicit def picklerEvent                    = Rev3.picklerEvent
  @inline implicit def picklerProject                  = Rev3.picklerProject
  @inline implicit def picklerProjectAndOrd            = Rev3.picklerProjectAndOrd
  @inline implicit def picklerVerifiedEvent            = Rev3.picklerVerifiedEvent
  @inline implicit def picklerVerifiedEventSeq         = Rev3.picklerVerifiedEventSeq
  @inline implicit def picklerVerifiedEventNonEmptySeq = Rev3.picklerVerifiedEventNonEmptySeq

  val AtomPicklers      = Rev3.AtomPicklers
  val SavedViewPicklers = Rev1.SavedViewPicklers
}

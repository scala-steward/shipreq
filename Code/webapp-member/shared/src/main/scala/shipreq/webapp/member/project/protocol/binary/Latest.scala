package shipreq.webapp.member.project.protocol.binary

/** This is a convenience for usages that don't need to care about versioning (eg. benchmarks, WW, tests).
  * It reduces the amount of busy-work required when bumping versions by allowing you to modify just this one
  * object rather than all uses that don't care and just need the latest.
  */
object Latest {

  @inline implicit def picklerEvent                    = v2.Rev1.picklerEvent
  @inline implicit def picklerProject                  = v2.Rev1.picklerProject
  @inline implicit def picklerVerifiedEvent            = v2.Rev1.picklerVerifiedEvent
  @inline implicit def picklerVerifiedEventSeq         = v2.Rev1.picklerVerifiedEventSeq
  @inline implicit def picklerVerifiedEventNonEmptySeq = v2.Rev1.picklerVerifiedEventNonEmptySeq
  @inline implicit def pickleValidFilter               = v2.Rev1.pickleValidFilter

  val AtomPicklers      = v1.Rev6.AtomPicklers
  val SavedViewPicklers = v2.Rev1.SavedViewPicklers
}

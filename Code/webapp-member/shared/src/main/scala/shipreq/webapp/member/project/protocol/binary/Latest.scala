package shipreq.webapp.member.project.protocol.binary

/** This is a convenience for usages that don't need to care about versioning (eg. benchmarks, WW, tests).
  * It reduces the amount of busy-work required when bumping versions by allowing you to modify just this one
  * object rather than all uses that don't care and just need the latest.
  */
object Latest {

  @inline implicit def picklerEvent                    = v3.Rev0.picklerEvent
  @inline implicit def picklerProject                  = v3.Rev0.picklerProject
  @inline implicit def picklerVerifiedEvent            = v3.Rev0.picklerVerifiedEvent
  @inline implicit def picklerVerifiedEventSeq         = v3.Rev0.picklerVerifiedEventSeq
  @inline implicit def picklerVerifiedEventNonEmptySeq = v3.Rev0.picklerVerifiedEventNonEmptySeq
  @inline implicit def pickleValidFilter               = v3.Rev0.pickleValidFilter
  @inline implicit def pickleFormulaValid              = v3.Rev0.pickleFormulaValid

  val AtomPicklers      = v1.Rev6.AtomPicklers
  val SavedViewPicklers = v3.Rev0.SavedViewPicklers
}

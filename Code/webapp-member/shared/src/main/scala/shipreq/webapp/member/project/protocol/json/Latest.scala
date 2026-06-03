package shipreq.webapp.member.project.protocol.json

/** This is a convenience for usages that don't need to care about versioning (eg. benchmarks, WW, tests).
  * It reduces the amount of busy-work required when bumping versions by allowing you to modify just this one
  * object rather than all uses that don't care and just need the latest.
  */
object Latest {

  @inline implicit def decoderEvent         = v2.Rev1.decoderEvent
  @inline implicit def encoderEvent         = v2.Rev1.encoderEvent
  @inline implicit def decoderVerifiedEvent = v2.Rev1.decoderVerifiedEvent
  @inline implicit def encoderVerifiedEvent = v2.Rev1.encoderVerifiedEvent
  @inline implicit def codecValidFilter     = v1.Rev7.codecValidFilter

  val AtomCodecs      = v1.Rev6.AtomCodecs
  val SavedViewCodecs = v1.Rev7.SavedViewCodecs
}

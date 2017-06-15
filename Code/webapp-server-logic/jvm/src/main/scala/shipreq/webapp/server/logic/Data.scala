package shipreq.webapp.server.logic

import japgolly.univeq.UnivEq

final case class ProjectId(value: Long) // extends AnyVal - nope, it gets boxed

// Should really be using the one in TaskmanApi but this is being compiled to JS which TaskmanApi isn't
final case class UserId(value: Long) extends AnyVal
object UserId {
  implicit def univEq: UnivEq[UserId] = UnivEq.derive
}

final case class EventSeq(value: Int) extends AnyVal {
  def succ = EventSeq(value + 1)
}

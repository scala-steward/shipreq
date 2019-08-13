package shipreq.webapp.base.data

import japgolly.univeq.UnivEq

/** Location of a tag in a requirement. */
sealed trait ReqTagLoc

object ReqTagLoc {
  case object Tags extends ReqTagLoc

  final case class And[+A](loc: ReqTagLoc, value: A) {
    override def toString = s"$loc.and($value)"
  }

  implicit def univEqA[A: UnivEq]: UnivEq[And[A]] = UnivEq.derive
  implicit def univEq: UnivEq[ReqTagLoc] = UnivEq.derive
}

/** Location of (rich) text in a requirement. */
sealed trait ReqTextLoc
  extends ReqTagLoc // more efficient version of: case class ReqTagLoc.Text(ReqTextLoc)

object ReqTextLoc {
  case object Title                                              extends ReqTextLoc
  final case class CustomTextField(fieldId: CustomField.Text.Id) extends ReqTextLoc
  final case class UseCaseStep    (stepId: UseCaseStepId)        extends ReqTextLoc

  final case class And[+A](loc: ReqTextLoc, value: A) {
    override def toString = s"$loc.and($value)"
  }

  implicit def univEqA[A: UnivEq]: UnivEq[And[A]] = UnivEq.derive
  implicit def univEq: UnivEq[ReqTextLoc] = UnivEq.derive
}

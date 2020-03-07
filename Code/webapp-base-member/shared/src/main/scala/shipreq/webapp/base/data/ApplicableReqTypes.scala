package shipreq.webapp.base.data

import japgolly.univeq.UnivEq
import shipreq.base.util._

final case class ApplicableReqTypes(applicability: Applicability,
                                    reqTypes     : Set[ReqTypeId]) {
  def isEmpty: Boolean =
    reqTypes.isEmpty
}

object ApplicableReqTypes {
  implicit def univEq: UnivEq[ApplicableReqTypes] = UnivEq.derive

  val isEmpty: ApplicableReqTypes =
    apply(NotApplicable, Set.empty)
}
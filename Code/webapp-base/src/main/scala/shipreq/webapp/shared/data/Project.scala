package shipreq.webapp.shared.data

import shipreq.base.prop._

final case class CustomReqTypes(rev: Rev, data: List[CustomReqType])

final case class CustomIncmpTypes(rev: Rev, data: List[CustomIncmpType])

final case class Project(customIncmpTypes: CustomIncmpTypes, customReqTypes: CustomReqTypes) {
  def rev = customIncmpTypes.rev + customReqTypes.rev
  this assertSatisfies DataProp.project
}

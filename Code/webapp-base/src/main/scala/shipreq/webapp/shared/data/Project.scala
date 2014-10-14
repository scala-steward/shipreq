package shipreq.webapp.shared.data

import shipreq.base.prop._

final case class CustomReqTypes(
  rev: delta.Rev,
  data: List[CustomReqType])

final case class Project(customReqTypes: CustomReqTypes) {
  def rev = customReqTypes.rev

  this assertSatisfies DataProp.project
}

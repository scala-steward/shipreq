package shipreq.webapp.shared.data

final case class CustomReqTypes(
  rev: delta.Rev,
  data: Seq[CustReqType])

final case class Project(customReqTypes: CustomReqTypes) {
  def rev = customReqTypes.rev
}

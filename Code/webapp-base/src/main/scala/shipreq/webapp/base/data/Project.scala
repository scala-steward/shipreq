package shipreq.webapp.base.data

import monocle.Lens
import monocle.macros.Lenser

case class RevAnd[D](rev: Rev, data: D)

object RevAnd {
  def _data[D] = Lens((_: RevAnd[D]).data)(b => _.copy(data = b))
}

object Project {
  private[this] def l = Lenser[Project]
  val _customIssueTypes = l(_.customIssueTypes)
  val _customReqTypes   = l(_.customReqTypes)
}

final case class Project(customIssueTypes: RevAnd[CustomIssueTypeIMap],
                         customReqTypes:   RevAnd[CustomReqTypeIMap],
                         tags:             RevAnd[TagTree]) {
  import shipreq.prop._
  this assertSatisfies DataProp.project

  def rev = customIssueTypes.rev + customReqTypes.rev + tags.rev

  override def toString =
    Stream(customIssueTypes, customReqTypes, tags)
      .map("\n    " + _.toString.replace(" -> ", " → "))
      .mkString("Project(", "", "\n)")
}
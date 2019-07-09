package shipreq.webapp.client.project.app.issues

import japgolly.scalajs.react.Reusability
import shipreq.webapp.base.data._
import shipreq.webapp.base.issue._
import shipreq.webapp.base.UiText.{Issues => UI}
import shipreq.webapp.client.project.widgets.ViewReq

sealed trait Row {
  val issue: Issue
  val issueClassDesc: String
  // val fieldName: Option[String]
  // val actions: List[Action]

  def issueCategoryDesc = UI.category(issue.category)
}

object Row {
  final case class ForConfig(issue         : Issue,
                             issueClassDesc: String) extends Row

  final case class ForReq(issue         : Issue,
                          issueClassDesc: String,
                          req           : Req,
                          viewReq   :ViewReq.Data) extends Row

  final case class ForRcg(issue         : Issue,
                          issueClassDesc: String,
                          rcg           : LiveCodeGroup,
                          code          : ReqCode.Value) extends Row

  implicit def reusability: Reusability[Row] =
    Reusability.byRef

  def fromIssue(p: Project): Issue => Row = {
    val customFieldName = CustomField.nameP(p)

    def forReq(i: Issue, desc: String, r: Req) =
      ForReq(i, desc, r, ViewReq.Data.fromProject(r, p, HideDead))

    def forRcg(i: Issue, desc: String, g: LiveCodeGroup) =
      ForRcg(i, desc, g, p.content.reqCodes.reqCode(g.id))

    {
      case i: Issue.BlankCustomField =>
        val desc = UI.descBlankCustomField(customFieldName(i.field))
        forReq(i, desc, i.req)

      case i: Issue.BlankTitle =>
        forReq(i, UI.descBlankTitle, i.req)

      case i: Issue.BlankUseCaseStep =>
        forReq(i, UI.descBlankUseCaseStep, i.step.uc)

      case i: Issue.ConflictingTags =>
        val tag  = p.config.tags.needTagGroup(i.tagGroupId)
        val desc = UI.descConflictingTags(tag.name)
        forReq(i, desc, i.req)

      case i: Issue.DeadIssueTagInRcg =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descDeadIssueTag(it.key)
        forRcg(i, desc, i.rcg)

      case i: Issue.DeadIssueTagInReq =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descDeadIssueTag(it.key)
        forReq(i, desc, i.req)

      case i: Issue.DeadRefInRcg =>
        forRcg(i, UI.descDeadRef, i.rcg)

      case i: Issue.DeadRefInReq =>
        forReq(i, UI.descDeadRef, i.req)

      case i: Issue.DeadTag =>
        val desc = UI.descDeadTag(i.tag.key)
        forReq(i, desc, i.req)

      case i: Issue.EmptyCodeGroup =>
        forRcg(i, UI.descEmptyCodeGroup, i.rcg)

      case i: Issue.ImplicationRequired =>
        val reqType = p.config.reqTypes.need(i.req.reqTypeId)
        val desc = UI.descImplicationRequired(reqType.mnemonic)
        forReq(i, desc, i.req)

      case i: Issue.IssueTagInRcg =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descIssueTag(it.key)
        forRcg(i, desc, i.rcg)

      case i: Issue.IssueTagInReq =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descIssueTag(it.key)
        forReq(i, desc, i.req)

      case i: Issue.UninhabitableTagField =>
        val fieldName = i.field.name(p.config.tags.tree)
        val desc = UI.descUninhabitableTagField(fieldName)
        ForConfig(i, desc)
    }
  }
}

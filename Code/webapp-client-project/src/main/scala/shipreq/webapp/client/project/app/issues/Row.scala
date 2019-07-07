package shipreq.webapp.client.project.app.issues

import shipreq.webapp.base.data._
import shipreq.webapp.base.issue._
import shipreq.webapp.base.UiText.{Issues => UI}

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
                          req           : Req) extends Row

  final case class ForRcg(issue         : Issue,
                          issueClassDesc: String,
                          rcg           : LiveCodeGroup) extends Row

  def fromIssue(p: Project): Issue => Row = {
    val customFieldName = CustomField.nameP(p)

    {
      case i: Issue.BlankCustomField =>
        val desc = UI.descBlankCustomField(customFieldName(i.field))
        ForReq(i, desc, i.req)

      case i: Issue.BlankTitle =>
        ForReq(i, UI.descBlankTitle, i.req)

      case i: Issue.BlankUseCaseStep =>
        ForReq(i, UI.descBlankUseCaseStep, i.step.uc)

      case i: Issue.ConflictingTags =>
        val tag  = p.config.tags.needTagGroup(i.tagGroupId)
        val desc = UI.descConflictingTags(tag.name)
        ForReq(i, desc, i.req)

      case i: Issue.DeadIssueTagInRcg =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descDeadIssueTag(it.key)
        ForRcg(i, desc, i.rcg)

      case i: Issue.DeadIssueTagInReq =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descDeadIssueTag(it.key)
        ForReq(i, desc, i.req)

      case i: Issue.DeadRefInRcg =>
        ForRcg(i, UI.descDeadRef, i.rcg)

      case i: Issue.DeadRefInReq =>
        ForReq(i, UI.descDeadRef, i.req)

      case i: Issue.DeadTag =>
        val desc = UI.descDeadTag(i.tag.key)
        ForReq(i, desc, i.req)

      case i: Issue.EmptyCodeGroup =>
        ForRcg(i, UI.descEmptyCodeGroup, i.rcg)

      case i: Issue.ImplicationRequired =>
        val reqType = p.config.reqTypes.need(i.req.reqTypeId)
        val desc = UI.descImplicationRequired(reqType.mnemonic)
        ForReq(i, desc, i.req)

      case i: Issue.IssueTagInRcg =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descIssueTag(it.key)
        ForRcg(i, desc, i.rcg)

      case i: Issue.IssueTagInReq =>
        val it   = p.config.customIssueType(i.issue.typ)
        val desc = UI.descIssueTag(it.key)
        ForReq(i, desc, i.req)

      case i: Issue.UninhabitableTagField =>
        val fieldName = i.field.name(p.config.tags.tree)
        val desc = UI.descUninhabitableTagField(fieldName)
        ForConfig(i, desc)
    }
  }
}

package shipreq.webapp.member.project.event

import shipreq.webapp.base.data.{ProjectRole, UserId}
import shipreq.webapp.member.project.event.Event._

object EventPermission {

  /**
   * Note: the event is assumed to be valid. This is not the place to check the validity of events.
   */
  def requiredRole(author: UserId, event: Event): ProjectRole =
    event match {

      case _: ProjectNameSet
         | _: ProjectTemplateApply
         | _: ProjectDelete
         |    ProjectRestore
         =>
          ProjectRole.Admin

      case a: AccessUpdate =>
        if (a.userId ==* author && a.newRole.isEmpty)
          // Users are allowed to remove themselves from projects
          ProjectRole.min
        else
          // Otherwise, only admin can add/remove users to/from a project
          ProjectRole.Admin

      case _: ApplicableTagCreate
         | _: ApplicableTagCreateV1
         | _: ApplicableTagUpdate
         | _: ApplicableTagUpdateV1
         | _: CodeGroupCreate
         | _: CodeGroupsDelete
         | _: CodeGroupUpdate
         | _: ContentRestore
         | _: CustomIssueTypeCreate
         | _: CustomIssueTypeDelete
         | _: CustomIssueTypeRestore
         | _: CustomIssueTypeUpdate
         | _: CustomReqTypeCreate
         | _: CustomReqTypeCreateV1
         | _: CustomReqTypeDelete
         | _: CustomReqTypeDeleteHard
         | _: CustomReqTypeDeleteSoft
         | _: CustomReqTypeRestore
         | _: CustomReqTypeUpdate
         | _: CustomReqTypeUpdateV1
         | _: FieldCustomDelete
         | _: FieldCustomImpCreate
         | _: FieldCustomImpCreateV1
         | _: FieldCustomImpUpdate
         | _: FieldCustomImpUpdateV1
         | _: FieldCustomNumberCreate
         | _: FieldCustomNumberUpdate
         | _: FieldCustomRestore
         | _: FieldCustomTagCreate
         | _: FieldCustomTagCreateV1
         | _: FieldCustomTagUpdate
         | _: FieldCustomTagUpdateV1
         | _: FieldCustomTextCreate
         | _: FieldCustomTextCreateV1
         | _: FieldCustomTextUpdate
         | _: FieldCustomTextUpdateV1
         | _: FieldReposition
         | _: FieldStaticAdd
         | _: FieldStaticRemove
         | _: GenericReqCreate
         | _: GenericReqTitleSet
         | _: GenericReqTypeSet
         | _: ManualIssueCreate
         | _: ManualIssueDelete
         | _: ManualIssueUpdate
         | _: ReqCodesPatch
         | _: ReqFieldCustomNumberSet
         | _: ReqFieldCustomTextSet
         | _: ReqImplicationsPatch
         | _: ReqsDelete
         | _: ReqTagsPatch
         | _: SavedViewCreate
         | _: SavedViewCreateV1
         | _: SavedViewDefaultSet
         | _: SavedViewDelete
         | _: SavedViewUpdate
         | _: SavedViewUpdateV1
         | _: TagDelete
         | _: TagGroupCreate
         | _: TagGroupUpdate
         | _: TagRestore
         | _: UseCaseCreate
         | _: UseCaseStepCreate
         | _: UseCaseStepDelete
         | _: UseCaseStepRestore
         | _: UseCaseStepShiftLeft
         | _: UseCaseStepShiftRight
         | _: UseCaseStepUpdate
         | _: UseCaseTitleSet
        =>
          ProjectRole.Collaborator
    }
}

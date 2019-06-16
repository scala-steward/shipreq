package shipreq.webapp.base.event

import japgolly.univeq._
import shipreq.webapp.base.data._

final case class EventSeqSummary(
    customIssueTypes : Set[CustomIssueTypeId],
    customFieldTypes : Set[CustomFieldId],
    customReqTypesCU : Set[CustomReqTypeId],
    customReqTypesDR : Set[CustomReqTypeId],
    tagGroups        : Set[TagGroupId],
    applicableTags   : Set[ApplicableTagId],
    staticFields     : Set[StaticField],
    genericReqs      : Set[GenericReqId],
    useCasesExclSteps: Set[UseCaseId],
    useCaseSteps     : Set[UseCaseStepId],
    ) {

  lazy val customReqTypes: Set[CustomReqTypeId] =
    customReqTypesCU ++ customReqTypesDR

  val tagsChanged: Boolean =
    applicableTags.nonEmpty || tagGroups.nonEmpty

  val fieldNamesChanged: Boolean =
    tagsChanged || customFieldTypes.nonEmpty || customReqTypesCU.nonEmpty

  lazy val tags: Set[TagId] =
    applicableTags ++ tagGroups

  lazy val reqsExclUseCaseSteps: Set[ReqId] =
    genericReqs ++ useCasesExclSteps

  def withProject(p: Project): EventSeqSummary.WithProject =
    EventSeqSummary.WithProject(this, p)
}

object EventSeqSummary {
  
  def apply(events: TraversableOnce[Event]): EventSeqSummary = {
    val b = new MutableBuilder
    b ++= events
    b.result()
  }

  final case class WithProject(summary: EventSeqSummary, project: Project) {

    lazy val useCasesChangedBySteps: Set[UseCaseId] = {
      val stepIndex = project.content.reqs.useCases.stepIndex
      summary.useCaseSteps.map(stepIndex(_).useCaseId)
    }

    lazy val useCases: Set[UseCaseId] =
      summary.useCasesExclSteps ++ useCasesChangedBySteps

    lazy val reqsAffectedByReqTypeChanges: Set[ReqId] =
      summary.customReqTypesDR.flatMap(project.content.reqs.reqsByType(_).iterator.map(_.id))

    lazy val reqs: Set[ReqId] =
      summary.reqsExclUseCaseSteps ++ useCasesChangedBySteps ++ reqsAffectedByReqTypeChanges
  }

  implicit def exportSummaryToWithProject(w: WithProject) = w.summary

  // ===================================================================================================================

  private final class MutableBuilder {
    private var customIssueTypes  = UnivEq.emptySet[CustomIssueTypeId]
    private var customFieldTypes  = UnivEq.emptySet[CustomFieldId]
    private var customReqTypesCU  = UnivEq.emptySet[CustomReqTypeId]
    private var customReqTypesDR  = UnivEq.emptySet[CustomReqTypeId]
    private var tagGroups         = UnivEq.emptySet[TagGroupId]
    private var applicableTags    = UnivEq.emptySet[ApplicableTagId]
    private var staticFields      = UnivEq.emptySet[StaticField]
    private var genericReqs       = UnivEq.emptySet[GenericReqId]
    private var useCasesExclSteps = UnivEq.emptySet[UseCaseId]
    private var useCaseSteps      = UnivEq.emptySet[UseCaseStepId]

    private val addReq: ReqId => Unit = {
      case i: GenericReqId => genericReqs       += i
      case i: UseCaseId    => useCasesExclSteps += i
    }

    private val addTag: TagId => Unit = {
      case i: TagGroupId      => tagGroups      += i
      case i: ApplicableTagId => applicableTags += i
    }

    def ++=(events: TraversableOnce[Event]): Unit =
      events.foreach(+=)
    
    val += : Event => Unit = {
      case e: Event.GenericReqCreate       => genericReqs += e.id
      case e: Event.GenericReqTitleSet     => genericReqs += e.id
      case e: Event.GenericReqTypeSet      => genericReqs += e.id

      case e: Event.UseCaseCreate          => useCasesExclSteps += e.id
      case e: Event.UseCaseStepCreate      => useCaseSteps += e.id
      case e: Event.UseCaseStepDelete      => useCaseSteps += e.id
      case e: Event.UseCaseStepRestore     => useCaseSteps += e.id
      case e: Event.UseCaseStepShiftLeft   => useCaseSteps += e.id
      case e: Event.UseCaseStepShiftRight  => useCaseSteps += e.id
      case e: Event.UseCaseStepUpdate      => useCaseSteps += e.id
      case e: Event.UseCaseTitleSet        => useCasesExclSteps += e.id

      case e: Event.ContentRestore         => e.reqs.foreach(addReq)
      case e: Event.ReqCodesPatch          => addReq(e.id)
      case e: Event.ReqFieldCustomTextSet  => addReq(e.id)
      case e: Event.ReqImplicationsPatch   => addReq(e.id)
      case e: Event.ReqTagsPatch           => addReq(e.id)
      case e: Event.ReqsDelete             => e.reqs.foreach(addReq)

      case e: Event.CustomIssueTypeCreate  => customIssueTypes += e.id
      case e: Event.CustomIssueTypeDelete  => customIssueTypes += e.id
      case e: Event.CustomIssueTypeRestore => customIssueTypes += e.id
      case e: Event.CustomIssueTypeUpdate  => customIssueTypes += e.id

      case e: Event.FieldCustomDelete      => customFieldTypes += e.id
      case e: Event.FieldCustomImpCreate   => customFieldTypes += e.id
      case e: Event.FieldCustomImpUpdate   => customFieldTypes += e.id
      case e: Event.FieldCustomRestore     => customFieldTypes += e.id
      case e: Event.FieldCustomTagCreate   => customFieldTypes += e.id
      case e: Event.FieldCustomTagUpdate   => customFieldTypes += e.id
      case e: Event.FieldCustomTextCreate  => customFieldTypes += e.id
      case e: Event.FieldCustomTextUpdate  => customFieldTypes += e.id

      case e: Event.CustomReqTypeCreate    => customReqTypesCU += e.id
      case e: Event.CustomReqTypeDelete    => customReqTypesDR += e.id // Affects GenericReq.live & ReqCodes
      case e: Event.CustomReqTypeRestore   => customReqTypesDR += e.id // Affects GenericReq.live & ReqCodes
      case e: Event.CustomReqTypeUpdate    => customReqTypesCU += e.id

      case e: Event.ApplicableTagCreate    => applicableTags += e.id
      case e: Event.ApplicableTagUpdate    => applicableTags += e.id
      case e: Event.TagGroupCreate         => tagGroups += e.id
      case e: Event.TagGroupUpdate         => tagGroups += e.id
      case e: Event.TagDelete              => addTag(e.id)
      case e: Event.TagRestore             => addTag(e.id)

      case e: Event.FieldStaticAdd         => staticFields += e.f
      case e: Event.FieldStaticRemove      => staticFields += e.f

      case e: Event.ProjectTemplateApply   => this ++= e.template.events

      case _: Event.FieldReposition
         | _: Event.ProjectNameSet
         | _: Event.CodeGroupCreate
         | _: Event.CodeGroupsDelete
         | _: Event.CodeGroupUpdate
         | _: Event.SavedViewCreate
         | _: Event.SavedViewDefaultSet
         | _: Event.SavedViewDelete
         | _: Event.SavedViewUpdate        => ()
    }
    
    def result(): EventSeqSummary =
      EventSeqSummary(
        customIssueTypes  = customIssueTypes,
        customFieldTypes  = customFieldTypes,
        customReqTypesCU  = customReqTypesCU,
        customReqTypesDR  = customReqTypesDR,
        tagGroups         = tagGroups       ,
        applicableTags    = applicableTags  ,
        staticFields      = staticFields    ,
        genericReqs       = genericReqs     ,
        useCasesExclSteps = useCasesExclSteps        ,
        useCaseSteps      = useCaseSteps    ,
      )
  }

}
package shipreq.webapp.base.event

import japgolly.microlibs.stdlib_ext.MutableArray
import japgolly.univeq._
import shipreq.webapp.base.data._

/** Summary of a sequence of events.
  *
  * Where there are CU and DR suffixes, CU = created/updated, DR = deleted/restored.
  */
final case class EventSeqSummary(
    customIssueTypes : EventSeqSummary.CUDR[CustomIssueTypeId],
    customFieldTypes : EventSeqSummary.CUDR[CustomFieldId],
    customReqTypes   : EventSeqSummary.CUDR[CustomReqTypeId],
    tagGroups        : EventSeqSummary.CUDR[TagGroupId],
    applicableTags   : EventSeqSummary.CUDR[ApplicableTagId],
    reqCodeGroups    : EventSeqSummary.CUDR[ReqCodeGroupId],
    staticFields     : EventSeqSummary.CUDR[StaticField],
    genericReqs      : EventSeqSummary.CUDR[GenericReqId],
    useCasesExclSteps: EventSeqSummary.CUDR[UseCaseId],
    useCaseSteps     : EventSeqSummary.CUDR[UseCaseStepId],
    contentLiveDeps  : Boolean,
    ) {

  override def toString =
    s"""
       |EventSeqSummary(
       |  customIssueTypes  = ${customIssueTypes .show(_.value)}
       |  customFieldTypes  = ${customFieldTypes .show(_.value)}
       |  customReqTypes    = ${customReqTypes   .show(_.value)}
       |  tagGroups         = ${tagGroups        .show(_.value)}
       |  applicableTags    = ${applicableTags   .show(_.value)}
       |  reqCodeGroups     = ${reqCodeGroups    .show(_.value)}
       |  staticFields      = ${staticFields     .show(_.toString)}
       |  genericReqs       = ${genericReqs      .show(_.value)}
       |  useCasesExclSteps = ${useCasesExclSteps.show(_.value)}
       |  useCaseSteps      = ${useCaseSteps     .show(_.value)}
       |  contentLiveDeps   = $contentLiveDeps ){
       |  customTextFields  = $customTextFields
       |  hasTags           = $hasTags }
     """.stripMargin

  val customTextFields: Set[CustomField.Text.Id] =
    customFieldTypes.all.collect {
      case f: CustomField.Text.Id => f
    }

  val hasTagsCU: Boolean =
    applicableTags.hasCU || tagGroups.hasCU

  val hasTagsDR: Boolean =
    applicableTags.hasDR || tagGroups.hasDR

  val hasTags: Boolean =
    hasTagsCU || hasTagsDR

  val fieldNamesChanged: Boolean =
    hasTags || customFieldTypes.hasAny || customReqTypes.hasCU

  lazy val reqsExclUseCaseSteps: Set[ReqId] =
    genericReqs.all ++ useCasesExclSteps.all

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
      summary.useCaseSteps.all.map(stepIndex(_).useCaseId)
    }

    lazy val useCases: Set[UseCaseId] =
      summary.useCasesExclSteps.all ++ useCasesChangedBySteps

    lazy val reqsAffectedByReqTypeChanges: Set[ReqId] =
      summary.customReqTypes.DR.flatMap(project.content.reqs.reqsByType(_).iterator.map(_.id))

    lazy val reqs: Set[ReqId] =
      summary.reqsExclUseCaseSteps ++ useCasesChangedBySteps ++ reqsAffectedByReqTypeChanges
  }

  implicit def exportSummaryToWithProject(w: WithProject) = w.summary

  // ===================================================================================================================

  final case class CUDR[A](created : Set[A],
                           updated : Set[A],
                           deleted : Set[A],
                           restored: Set[A]) {

    override def toString = show(identity)

    def show(showValue: A => Any) = {
      def show(as: Set[A]) = MutableArray(as.iterator.map("" + showValue(_))).sort.mkString("{", ",", "}")
      s"CUDR(C=${show(created)}, U=${show(updated)}, D=${show(deleted)}, R=${show(restored)})"
    }

    private def add(x: Set[A], y: Set[A]): Set[A] =
      if (x.isEmpty) y
      else if (y.isEmpty) x
      else x ++ y

    val CU  = add(created, updated)
    val DR  = add(deleted, restored)
    val all = add(CU, DR)

    @inline def hasC   = created.nonEmpty
    @inline def hasU   = updated.nonEmpty
    @inline def hasD   = deleted.nonEmpty
    @inline def hasR   = restored.nonEmpty
    @inline def hasCU  = CU.nonEmpty
    @inline def hasDR  = DR.nonEmpty
    @inline def hasAny = all.nonEmpty
  }

  object CUDR {
    sealed trait Field
    object Field {
      case object Created extends Field
      case object Updated extends Field
      case object Deleted extends Field
      case object Restored extends Field
    }

    final class Mutable[A: UnivEq] {
      var created : Set[A] = Set.empty
      var updated : Set[A] = Set.empty
      var deleted : Set[A] = Set.empty
      var restored: Set[A] = Set.empty

      def add(f: Field, a: A): Unit =
        f match {
          case Field.Created  => created  += a
          case Field.Updated  => updated  += a
          case Field.Deleted  => deleted  += a
          case Field.Restored => restored += a
        }

      def result(): CUDR[A] =
        CUDR(
          created = created,
          updated = updated,
          deleted = deleted,
          restored = restored)
    }
  }

  private final class MutableBuilder {
    private[this] val customIssueTypes  = new CUDR.Mutable[CustomIssueTypeId]
    private[this] val customFieldTypes  = new CUDR.Mutable[CustomFieldId    ]
    private[this] val customReqTypes    = new CUDR.Mutable[CustomReqTypeId  ]
    private[this] val tagGroups         = new CUDR.Mutable[TagGroupId       ]
    private[this] val applicableTags    = new CUDR.Mutable[ApplicableTagId  ]
    private[this] val reqCodeGroups     = new CUDR.Mutable[ReqCodeGroupId   ]
    private[this] val staticFields      = new CUDR.Mutable[StaticField]
    private[this] val genericReqs       = new CUDR.Mutable[GenericReqId]
    private[this] val useCasesExclSteps = new CUDR.Mutable[UseCaseId]
    private[this] val useCaseSteps      = new CUDR.Mutable[UseCaseStepId]
    private var contentLiveDeps         = false

    import CUDR.Field._

    private def req(id: ReqId, f: CUDR.Field): Unit = id match {
      case i: GenericReqId => genericReqs      .add(f, i)
      case i: UseCaseId    => useCasesExclSteps.add(f, i)
    }

    private def tag(id: TagId, f: CUDR.Field): Unit = id match {
      case i: TagGroupId      => tagGroups     .add(f, i)
      case i: ApplicableTagId => applicableTags.add(f, i)
    }

    def ++=(events: TraversableOnce[Event]): Unit =
      events.foreach(+=)
    
    val += : Event => Unit = {

      case e: Event.ContentRestore =>
        e.reqs.foreach(req(_, Restored))
        reqCodeGroups.restored ++= e.codeGroups

      case e: Event.ReqsDelete =>
        e.reqs.foreach(req(_, Deleted))
        reqCodeGroups.deleted ++= e.codeGroups

      case e: Event.ReqImplicationsPatch =>
        req(e.id, Updated)
        e.patch.added.foreach(req(_, Updated))
        e.patch.removed.foreach(req(_, Updated))

      case e: Event.CustomReqTypeDelete =>
        customReqTypes.deleted += e.id
        contentLiveDeps = true

      case e: Event.CustomReqTypeRestore =>
        customReqTypes.restored += e.id
        contentLiveDeps = true

      case e: Event.GenericReqTypeSet =>
        genericReqs.updated += e.id
        contentLiveDeps = true

      case e: Event.ApplicableTagCreate    => applicableTags.created += e.id
      case e: Event.ApplicableTagUpdate    => applicableTags.updated += e.id
      case e: Event.CodeGroupCreate        => reqCodeGroups.created += e.id
      case e: Event.CodeGroupsDelete       => reqCodeGroups.deleted ++= e.ids.whole
      case e: Event.CodeGroupUpdate        => reqCodeGroups.updated += e.id
      case e: Event.CustomIssueTypeCreate  => customIssueTypes.created += e.id
      case e: Event.CustomIssueTypeDelete  => customIssueTypes.deleted += e.id
      case e: Event.CustomIssueTypeRestore => customIssueTypes.restored += e.id
      case e: Event.CustomIssueTypeUpdate  => customIssueTypes.updated += e.id
      case e: Event.CustomReqTypeCreate    => customReqTypes.created += e.id
      case e: Event.CustomReqTypeUpdate    => customReqTypes.updated += e.id
      case e: Event.FieldCustomDelete      => customFieldTypes.deleted += e.id
      case e: Event.FieldCustomImpCreate   => customFieldTypes.created += e.id
      case e: Event.FieldCustomImpUpdate   => customFieldTypes.updated += e.id
      case e: Event.FieldCustomRestore     => customFieldTypes.restored += e.id
      case e: Event.FieldCustomTagCreate   => customFieldTypes.created += e.id
      case e: Event.FieldCustomTagUpdate   => customFieldTypes.updated += e.id
      case e: Event.FieldCustomTextCreate  => customFieldTypes.created += e.id
      case e: Event.FieldCustomTextUpdate  => customFieldTypes.updated += e.id
      case e: Event.FieldStaticAdd         => staticFields.created += e.f
      case e: Event.FieldStaticRemove      => staticFields.deleted += e.f
      case e: Event.GenericReqCreate       => genericReqs.created += e.id
      case e: Event.GenericReqTitleSet     => genericReqs.updated += e.id
      case e: Event.ProjectTemplateApply   => this ++= e.template.events
      case e: Event.ReqCodesPatch          => req(e.id, Updated)
      case e: Event.ReqFieldCustomTextSet  => req(e.id, Updated)
      case e: Event.ReqTagsPatch           => req(e.id, Updated)
      case e: Event.TagDelete              => tag(e.id, Deleted)
      case e: Event.TagGroupCreate         => tagGroups.created += e.id
      case e: Event.TagGroupUpdate         => tagGroups.updated += e.id
      case e: Event.TagRestore             => tag(e.id, Restored)
      case e: Event.UseCaseCreate          => useCasesExclSteps.created += e.id
      case e: Event.UseCaseStepCreate      => useCaseSteps.created += e.id
      case e: Event.UseCaseStepDelete      => useCaseSteps.deleted += e.id
      case e: Event.UseCaseStepRestore     => useCaseSteps.restored += e.id
      case e: Event.UseCaseStepShiftLeft   => useCaseSteps.updated += e.id // ?
      case e: Event.UseCaseStepShiftRight  => useCaseSteps.updated += e.id // ?
      case e: Event.UseCaseStepUpdate      => useCaseSteps.updated += e.id
      case e: Event.UseCaseTitleSet        => useCasesExclSteps.updated += e.id

      case _: Event.FieldReposition
         | _: Event.ProjectNameSet
         | _: Event.SavedViewCreate
         | _: Event.SavedViewDefaultSet
         | _: Event.SavedViewDelete
         | _: Event.SavedViewUpdate        => ()
    }
    
    def result(): EventSeqSummary =
      EventSeqSummary(
        customIssueTypes  = customIssueTypes .result(),
        customFieldTypes  = customFieldTypes .result(),
        customReqTypes    = customReqTypes   .result(),
        tagGroups         = tagGroups        .result(),
        applicableTags    = applicableTags   .result(),
        reqCodeGroups     = reqCodeGroups    .result(),
        staticFields      = staticFields     .result(),
        genericReqs       = genericReqs      .result(),
        useCasesExclSteps = useCasesExclSteps.result(),
        useCaseSteps      = useCaseSteps     .result(),
        contentLiveDeps   = contentLiveDeps,
      )
  }

}
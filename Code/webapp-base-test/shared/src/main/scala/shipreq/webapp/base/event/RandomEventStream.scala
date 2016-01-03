package shipreq.webapp.base.event

import nyaya.gen._
import nyaya.util.Multimap
import shipreq.base.test.BaseUtilGen._
import shipreq.base.test.IncCounter
import shipreq.base.util._
import shipreq.webapp.base.RandomData
import shipreq.webapp.base.data._
import shipreq.webapp.base.event.Event.NESD
import shipreq.webapp.base.hash._
import shipreq.webapp.base.test.WebappBaseGen._
import shipreq.webapp.base.text.Text
import RandomData.{fieldRefKey, hashRefKey, implicationRequired, mandatory, mutexChildren}
import RandomData.{reqCode, reqTypeMnemonic, TextGen, TextGenExt, unicodeString1}
import ScalaExt._
import UtilMacros._

/**
  * Generates a random event stream that can be successfully applied.
  *
  * This differs from the events that [[RandomData]] can generate which are only valid in isolation and often don't
  * make sense as a consecutive stream.
  */
object RandomEventStream {

  def nextEvent(p: Project): Gen[Event] = {
    ???
  }

}

/*
Don't repeat logic (which can be very complicated) to determine whether an event is applicable.

Generate best-guesses using simple invariants (mostly around IDs),
then apply and see if it worked. If not, repeat (so BindRec).

 */

class GenSuccEvent(p: Project) {

  private val cfg = p.config

  private implicit def autoGenToOptionGen[A](g: Gen[A]): Option[Gen[A]] = Some(g)

  private def deletionAction(l: Live): DeletionAction =
    if (l :: Live) Delete else Restore

  val (staticFieldsToDel, staticFieldsToAdd) =
    StaticField.values.whole partition cfg.fields.staticFieldSet.contains

  val nextReqId: Gen[Int] =
    IncCounter genInt p.idCeilings.req

  val nextGenericReqId: Gen[GenericReqId] =
    nextReqId map GenericReqId

  val nextReqCodeId: Gen[ReqCodeId] =
    IncCounter genInt p.idCeilings.reqCode map ReqCodeId

  val nextCustomIssueTypeId: Gen[CustomIssueTypeId] =
    IncCounter genInt p.idCeilings.customIssueType map CustomIssueTypeId

  val nextCustomReqTypeId: Gen[CustomReqTypeId] =
    IncCounter genInt p.idCeilings.customReqType map CustomReqTypeId

  val nextCustomField: Gen[Int] =
    IncCounter genInt p.idCeilings.customField

  val nextCustomFieldImplicationId: Gen[CustomField.Implication.Id] =
    nextCustomField map CustomField.Implication.Id

  val nextCustomFieldTagId: Gen[CustomField.Tag.Id] =
    nextCustomField map CustomField.Tag.Id

  val nextCustomFieldTextId: Gen[CustomField.Text.Id] =
    nextCustomField map CustomField.Text.Id

  val nextTagId: Gen[Int] =
    IncCounter genInt p.idCeilings.tag

  val nextApplicableTagId: Gen[ApplicableTagId] =
    nextTagId map ApplicableTagId

  val nextTagGroupId: Gen[TagGroupId] =
    nextTagId map TagGroupId

  lazy val existingFieldId: Gen[FieldId] =
    Gen.choose_!(p.config.fields.order)

  lazy val existingTagId: Option[Gen[TagId]] =
    Gen.tryGenChoose(p.config.tags.keysIterator)

  lazy val existingApplicableTagId: Option[Gen[ApplicableTagId]] =
    Gen.tryGenChoose(p.config.tags.keysIterator.filterT[ApplicableTagId])

  lazy val existingTagGroupId: Option[Gen[TagGroupId]] =
    Gen.tryGenChoose(p.config.tags.keysIterator.filterT[TagGroupId])

  def tagChildren: Gen[TagInTree.Children] =
    existingTagId match {
      case Some(g) => g.vector(0 to 3)
      case None    => Gen pure Vector.empty
    }

  def tagParents: Gen[TagInTree.Parents] =
    existingTagId match {
      case Some(g) => g.option.mapBy(g)(0 to 3)
      case None    => Gen pure Map.empty
    }

  lazy val existingReqTypeId: Gen[ReqTypeId] =
    Gen.chooseNE(StaticReqType.values.map(_.reqTypeId) ++ cfg.customReqTypes.keySet)

  lazy val existingCustomReqTypeId: Option[Gen[CustomReqTypeId]] =
    Gen.tryGenChoose(cfg.customReqTypes.keySet)

  lazy val applicableReqTypes: Gen[Field.ApplicableReqTypes] =
    RandomData.applicableReqTypes(cfg.customReqTypes.keySet)

  lazy val existingReqId: Option[Gen[ReqId]] =
    Gen.tryGenChoose(p.reqs.reqs.keysIterator)

  lazy val existingLiveReqId: Option[Gen[ReqId]] =
    Gen.tryGenChoose(p.reqs.reqs.valuesIterator.filter(_.live(cfg.customReqTypes) :: Live).map(_.id))

  lazy val existingGenericReqId: Option[Gen[GenericReqId]] =
    Gen.tryGenChoose(p.reqs.genericReqs.keySet)

  lazy val existingReqCodeId: Option[Gen[ReqCodeId]] =
    Gen.tryGenChoose(p.reqCodes.idList)

  lazy val existingLiveReqCodeId: Option[Gen[ReqCodeId]] =
    Gen.tryGenChoose(p.reqCodes.idSet -- p.reqCodes.inactiveIdsByReqId.allValues)

  lazy val existingDeadReqCodeId: Option[Gen[ReqCodeId]] =
    Gen.tryGenChoose(p.reqCodes.inactiveIdsByReqId.allValues)

  lazy val existingCustomIssueTypeId: Option[Gen[CustomIssueTypeId]] =
    Gen.tryGenChoose(cfg.customIssueTypes.keySet)

  lazy val existingCustomFieldId: Option[Gen[CustomFieldId]] =
    Gen.tryGenChoose(cfg.fields.customFields.keySet)

  lazy val existingCustomFieldImpId: Option[Gen[CustomField.Implication.Id]] =
    Gen.tryGenChoose(cfg.customImpFields.map(_.id))

  lazy val existingCustomFieldTagId: Option[Gen[CustomField.Tag.Id]] =
    Gen.tryGenChoose(cfg.customTagFields.map(_.id))

  lazy val existingCustomFieldTextId: Option[Gen[CustomField.Text.Id]] =
    Gen.tryGenChoose(cfg.customTextFields.map(_.id))

  lazy val customTextFieldText =
    TextGen.customTextFieldAtom(existingReqId, existingReqCodeId, existingCustomIssueTypeId, existingApplicableTagId).text

  lazy val newReqCodeIdAndValue =
    Gen.apply2(ReqCode.IdAndValue)(nextReqCodeId, reqCode.value)

  lazy val reqCodeGroupTitle =
    TextGen.reqCodeGroupTitleAtom(existingReqId, existingReqCodeId, existingCustomIssueTypeId).text

  private lazy val genericReqTitleAtom =
    TextGen.genericReqTitleAtom(existingReqId, existingReqCodeId, existingCustomIssueTypeId, existingApplicableTagId)

  lazy val genericReqTitle =
    genericReqTitleAtom.text

  lazy val genericReqTitle1 =
    genericReqTitleAtom.text1(Text.GenericReqTitle)

  lazy val deletionReason =
    TextGen.deletionReasonAtom(existingReqId, existingReqCodeId, existingApplicableTagId).text

  object customIssueTypeGD extends GenericDataGen(CustomIssueTypeGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Key  => hashRefKey            map Key .apply
      case Desc => unicodeString1.option map Desc.apply
    }
  }

  object customReqTypeGD extends GenericDataGen(CustomReqTypeGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Name        => unicodeString1      map Name       .apply
      case Imp         => implicationRequired map Imp        .apply
      case gd.Mnemonic => reqTypeMnemonic     map gd.Mnemonic.apply
    }
  }

  object customTextFieldGD extends GenericDataGen(CustomTextFieldGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Name      => unicodeString1     map Name     .apply
      case Key       => fieldRefKey        map Key      .apply
      case Mandatory => mandatory          map Mandatory.apply
      case ReqTypes  => applicableReqTypes map ReqTypes .apply
    }
  }

  object customTagFieldGD extends GenericDataOptionGen(CustomTagFieldGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case TagId     => existingTagId map (_ map TagId    .apply)
      case Mandatory => mandatory            map Mandatory.apply
      case ReqTypes  => applicableReqTypes   map ReqTypes .apply
    }
  }

  object customImpFieldGD extends GenericDataGen(CustomImpFieldGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case ReqTypeId => existingReqTypeId  map ReqTypeId.apply
      case Mandatory => mandatory          map Mandatory.apply
      case ReqTypes  => applicableReqTypes map ReqTypes .apply
    }
  }

  object createGenericReqGD extends GenericDataOptionGen(CreateGenericReqGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Title    => genericReqTitle1                     map Title   .apply
      case ReqCodes => newReqCodeIdAndValue          .nes   map ReqCodes.apply
      case Tags     => existingApplicableTagId map (_.nes   map Tags    .apply)
      case ImpSrcs  => existingReqId           map (_.nes   map ImpSrcs .apply)
      case ImpTgts  => existingReqId           map (_.nes   map ImpTgts .apply)
    }
  }

  object reqCodeGroupGD extends GenericDataGen(ReqCodeGroupGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Code  => reqCode.value     map Code .apply
      case Title => reqCodeGroupTitle map Title.apply
    }
  }

  object applicableTagGD extends GenericDataGen(ApplicableTagGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Name     => unicodeString1        map Name    .apply
      case Desc     => unicodeString1.option map Desc    .apply
      case Key      => hashRefKey            map Key     .apply
      case Children => tagChildren           map Children.apply
      case Parents  => tagParents            map Parents .apply
    }
  }

  object tagGroupGD extends GenericDataGen(TagGroupGD) {
    import gd._
    override def valueFor(a: Attr) = a match {
      case Name          => unicodeString1        map Name         .apply
      case Desc          => unicodeString1.option map Desc         .apply
      case MutexChildren => mutexChildren         map MutexChildren.apply
      case Children      => tagChildren           map Children     .apply
      case Parents       => tagParents            map Parents      .apply
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  def addStaticField: Option[Gen[AddStaticField]] =
    Gen.tryGenChoose(staticFieldsToAdd).map(_ map AddStaticField)

  def applyTemplate: Option[Gen[ApplyTemplate]] =
    if (p eq Project.empty)
      None
    else
      Some(RandomData.events.applyTemplate)

  def createApplicableTag: Gen[CreateApplicableTag] =
    Gen.apply2(CreateApplicableTag)(nextApplicableTagId, applicableTagGD.nonEmptyValues)

  def createCustomImpField: Gen[CreateCustomImpField] =
    Gen.apply2(CreateCustomImpField)(nextCustomFieldImplicationId, customImpFieldGD.nonEmptyValues)

  def createCustomIssueType: Gen[CreateCustomIssueType] =
    Gen.apply2(CreateCustomIssueType)(nextCustomIssueTypeId, customIssueTypeGD.nonEmptyValues)

  def createCustomReqType: Gen[CreateCustomReqType] =
    Gen.apply2(CreateCustomReqType)(nextCustomReqTypeId, customReqTypeGD.nonEmptyValues)

  def createCustomTagField: Option[Gen[CreateCustomTagField]] =
    customTagFieldGD.nonEmptyValues.map(vs =>
      Gen.apply2(CreateCustomTagField)(nextCustomFieldTagId, vs))

  def createCustomTextField: Gen[CreateCustomTextField] =
    Gen.apply2(CreateCustomTextField)(nextCustomFieldTextId, customTextFieldGD.nonEmptyValues)

  def createGenericReq: Option[Gen[CreateGenericReq]] =
    existingCustomReqTypeId.map(reqTypeId =>
      Gen.apply3(CreateGenericReq)(nextGenericReqId, reqTypeId, createGenericReqGD.values))

  def createReqCodeGroup: Gen[CreateReqCodeGroup] =
    Gen.apply2(CreateReqCodeGroup)(nextReqCodeId, reqCodeGroupGD.nonEmptyValues)

  def createTagGroup: Gen[CreateTagGroup] =
    Gen.apply2(CreateTagGroup)(nextTagGroupId, tagGroupGD.nonEmptyValues)

  def deleteCustomField: Option[Gen[DeleteCustomField]] =
    existingCustomFieldId.map(_.map(id =>
      DeleteCustomField(id, deletionAction(cfg.fields.customFields.need(id) live cfg))))

  def deleteCustomIssueType: Option[Gen[DeleteCustomIssueType]] =
    existingCustomIssueTypeId.map(_.map(id =>
      DeleteCustomIssueType(id, deletionAction(cfg.customIssueTypes.need(id).live))))

  def deleteCustomReqType: Option[Gen[DeleteCustomReqType]] =
    existingCustomReqTypeId.map(_.map(id =>
      DeleteCustomReqType(id, deletionAction(cfg.customReqTypes.need(id).live))))

  def deleteReqCodeGroups: Option[Gen[DeleteReqCodeGroups]] =
    Gen.tryGenChooseLazily(p.reqCodes.groups.iterator.map(_.id))
      .map(_.nes map DeleteReqCodeGroups)

  def deleteReqs: Option[Gen[DeleteReqs]] =
    existingLiveReqId.map { reqId =>
      val codes = existingLiveReqCodeId.setE
      Gen.apply3(DeleteReqs)(reqId.nes, codes, deletionReason)
    }

  def deleteStaticField: Option[Gen[DeleteStaticField]] =
    Gen.tryGenChoose(staticFieldsToDel).map(_ map DeleteStaticField)

  def deleteTag: Option[Gen[DeleteTag]] =
    existingTagId.map(g =>
      g.map(id =>
        DeleteTag(id, deletionAction(cfg.tags.get(id).fold[Live](Live)(_.tag.live)))))

  private def patchImplications[A](cmd: (ReqId, NESD[ReqId]) => A, fwd: Boolean): Option[Gen[A]] =
    existingReqId.map(gReqId =>
      for {
        id <- gReqId
        ids <- gReqId.set1
      } yield {
        val sd = SetDiff.xor(p.implications.dir(fwd)(id), ids)
        cmd(id, NonEmpty force sd)
      }
    )

  def patchImplicationSrc: Option[Gen[PatchImplicationSrc]] =
    patchImplications(PatchImplicationSrc, false)

  def patchImplicationTgt: Option[Gen[PatchImplicationTgt]] =
    patchImplications(PatchImplicationTgt, true)

  def patchReqCodes: Option[Gen[PatchReqCodes]] =
    existingLiveReqId.map(gReqId =>
      for {
        reqId          ← gReqId
        inactiveValues = p.reqCodes.inactiveIdsByReqId(reqId)
        restore        ← Gen.tryGenChoose(inactiveValues.toVector).setE
        existingValues = p.reqCodes.activeReqCodesByReqId(reqId)
        existingIds    = existingValues.map(p.reqCodes(_).activeId.get)
        gExistingSet   = Gen.tryGenChoose(existingIds.toVector).setE
        remove         ← gExistingSet
        renameIds      ← Gen.tryGenChoose(remove.toVector).setE
        addIds         ← nextReqCodeId.list
        add            ← Gen sequence (addIds ++ renameIds).map(id => reqCode.value.strengthR(Set.empty[ReqCodeId] + id))
      } yield
        PatchReqCodes(reqId, remove, restore, Multimap(add.toMap))
    )

  def patchReqTags: Option[Gen[PatchReqTags]] =
    for {
      gId  <- existingLiveReqId
      gTag <- existingApplicableTagId
    } yield for {
      id   <- gId
      tags <- gTag.nes
    } yield {
      val sd = SetDiff.xor(p.reqTags(id), tags.whole)
      PatchReqTags(id, NonEmpty force sd)
    }

  def repositionField: Gen[RepositionField] =
    Gen.apply2(RepositionField)(existingFieldId, existingFieldId.option)

  def restoreContent: Option[Gen[RestoreContent]] = {
    val restorableReqIds = Gen.tryGenChoose[ReqId](
      p.reqs.reqs.valuesIterator.filter {
        case g: GenericReq => (g.liveExplicitly :: Dead) && (g.copy(liveExplicitly = Live).live(cfg.customReqTypes) :: Live)
      }.map(_.id).toVector)
    if (restorableReqIds.isEmpty && existingDeadReqCodeId.isEmpty)
      None
    else Some {
      val idSet = restorableReqIds.setE
      val codeSet = existingDeadReqCodeId.setE
      Gen.apply2(RestoreContent)(idSet, codeSet).flatMap(cmd =>
        if (cmd.reqs.nonEmpty || cmd.reqCodes.nonEmpty)
          Gen pure cmd
        else if (restorableReqIds.isDefined)
          restorableReqIds.get.nes.map(ids => RestoreContent(ids.whole, Set.empty))
        else
          existingDeadReqCodeId.get.nes.map(ids => RestoreContent(Set.empty, ids.whole))
      )
    }
  }

  def setCustomTextField: Option[Gen[SetCustomTextField]] =
    for {
      id  <- existingReqId
      fid <- existingCustomFieldTextId
    } yield
      Gen.apply3(SetCustomTextField)(id, fid, customTextFieldText)

  def setGenericReqTitle: Option[Gen[SetGenericReqTitle]] =
    existingGenericReqId.map(id =>
      Gen.apply2(SetGenericReqTitle)(id, genericReqTitle))

  def setGenericReqType: Option[Gen[SetGenericReqType]] =
    for {
      gId <- existingGenericReqId
      gRT <- existingCustomReqTypeId
    } yield
      Gen.apply2(SetGenericReqType)(gId, gRT)

  def updateApplicableTag: Option[Gen[UpdateApplicableTag]] =
    existingApplicableTagId.map(id =>
      Gen.apply2(UpdateApplicableTag)(id, applicableTagGD.nonEmptyValues))

  def updateCustomImpField: Option[Gen[UpdateCustomImpField]] =
    existingCustomFieldImpId.map(id =>
      Gen.apply2(UpdateCustomImpField)(id, customImpFieldGD.nonEmptyValues))

  def updateCustomIssueType: Option[Gen[UpdateCustomIssueType]] =
    existingCustomIssueTypeId.map(id =>
      Gen.apply2(UpdateCustomIssueType)(id, customIssueTypeGD.nonEmptyValues))

  def updateCustomReqType: Option[Gen[UpdateCustomReqType]] =
    existingCustomReqTypeId.map(id =>
      Gen.apply2(UpdateCustomReqType)(id, customReqTypeGD.nonEmptyValues))

  def updateCustomTagField: Option[Gen[UpdateCustomTagField]] =
    for {
      id <- existingCustomFieldTagId
      vs <- customTagFieldGD.nonEmptyValues
    } yield
      Gen.apply2(UpdateCustomTagField)(id, vs)

  def updateCustomTextField: Option[Gen[UpdateCustomTextField]] =
    existingCustomFieldTextId.map(id =>
      Gen.apply2(UpdateCustomTextField)(id, customTextFieldGD.nonEmptyValues))

  def updateReqCodeGroup: Option[Gen[UpdateReqCodeGroup]] =
    existingReqCodeId.map(id =>
      Gen.apply2(UpdateReqCodeGroup)(id, reqCodeGroupGD.nonEmptyValues))

  def updateTagGroup: Option[Gen[UpdateTagGroup]] =
    existingTagGroupId.map(id =>
      Gen.apply2(UpdateTagGroup)(id, tagGroupGD.nonEmptyValues))

  def eventGens: NonEmptyVector[Option[Gen[Event]]] =
    valuesForAdt[Event, Option[Gen[Event]]] {
      case _: AddStaticField        => addStaticField
      case _: ApplyTemplate         => applyTemplate
      case _: CreateApplicableTag   => createApplicableTag
      case _: CreateCustomImpField  => createCustomImpField
      case _: CreateCustomIssueType => createCustomIssueType
      case _: CreateCustomReqType   => createCustomReqType
      case _: CreateCustomTagField  => createCustomTagField
      case _: CreateCustomTextField => createCustomTextField
      case _: CreateGenericReq      => createGenericReq
      case _: CreateReqCodeGroup    => createReqCodeGroup
      case _: CreateTagGroup        => createTagGroup
      case _: DeleteCustomField     => deleteCustomField
      case _: DeleteCustomIssueType => deleteCustomIssueType
      case _: DeleteCustomReqType   => deleteCustomReqType
      case _: DeleteReqCodeGroups   => deleteReqCodeGroups
      case _: DeleteReqs            => deleteReqs
      case _: DeleteStaticField     => deleteStaticField
      case _: DeleteTag             => deleteTag
      case _: PatchImplicationSrc   => patchImplicationSrc
      case _: PatchImplicationTgt   => patchImplicationTgt
      case _: PatchReqCodes         => patchReqCodes
      case _: PatchReqTags          => patchReqTags
      case _: RepositionField       => repositionField
      case _: RestoreContent        => restoreContent
      case _: SetCustomTextField    => setCustomTextField
      case _: SetGenericReqTitle    => setGenericReqTitle
      case _: SetGenericReqType     => setGenericReqType
      case _: UpdateApplicableTag   => updateApplicableTag
      case _: UpdateCustomImpField  => updateCustomImpField
      case _: UpdateCustomIssueType => updateCustomIssueType
      case _: UpdateCustomReqType   => updateCustomReqType
      case _: UpdateCustomTagField  => updateCustomTagField
      case _: UpdateCustomTextField => updateCustomTextField
      case _: UpdateReqCodeGroup    => updateReqCodeGroup
      case _: UpdateTagGroup        => updateTagGroup
    }
}
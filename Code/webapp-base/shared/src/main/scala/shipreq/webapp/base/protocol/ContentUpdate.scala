package shipreq.webapp.base.protocol

import shipreq.base.util.{SetDiff, UnivEq}
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.Text
import shipreq.webapp.base.util.TypeclassDerivation._
import boopickle._, BoopickleMacros._, BinCodecGeneric._, BinCodecData._, AtomPicklers.instances._
import Text.Equality._

/**
 * Protocol for editing a project.
 *
 * Note: The project itself is never explicitly specified here.
 */
//object ProjectProtocol {
//}

/*
Send ProjectRev alongside cmd?

This could be useful to send back to client.
Same code could be used on server & client to patch projects.

Or are there changes (knock-on effects) that require new instructions that would be server→client only?
 */

/**
 * A description of, or command to, change to a Project's content.
 */
sealed trait ContentUpdate
object ContentUpdate {

  case class PatchReqTags        (id: ReqId, patch: SetDiff[ApplicableTagId]) extends ContentUpdate
  case class PatchImplicationSrc (id: ReqId, patch: SetDiff[ReqId])           extends ContentUpdate
  case class PatchImplicationTgt (id: ReqId, patch: SetDiff[ReqId])           extends ContentUpdate
  case class PatchReqCodes       (id: ReqId, patch: SetDiff[ReqCode.Value])   extends ContentUpdate

  case class SetGenericReqType   (id: GenericReqId, value: CustomReqTypeId) extends ContentUpdate
  case class SetReqCodeGroupCode (id: ReqCodeId,    value: ReqCode.Value)   extends ContentUpdate

  case class SetReqCodeGroupTitle(id: ReqCodeId,                              value: Text.ReqCodeGroupTitle.OptionalText) extends ContentUpdate
  case class SetGenericReqTitle  (id: GenericReqId,                           value: Text.GenericReqTitle.OptionalText)   extends ContentUpdate
  case class SetCustomTextField  (id: ReqId,        fid: CustomField.Text.Id, value: Text.CustomTextField.OptionalText)   extends ContentUpdate

  implicit val contentUpdateEquality: UnivEq[ContentUpdate] = { import AutoDerive._; deriveUnivEq }

  implicit val picklePatchReqTags        : Pickler[PatchReqTags]         = pickleCaseClass
  implicit val picklePatchImplicationSrc : Pickler[PatchImplicationSrc]  = pickleCaseClass
  implicit val picklePatchImplicationTgt : Pickler[PatchImplicationTgt]  = pickleCaseClass
  implicit val picklePatchReqCodes       : Pickler[PatchReqCodes]        = pickleCaseClass
  implicit val pickleSetGenericReqType   : Pickler[SetGenericReqType]    = pickleCaseClass
  implicit val pickleSetReqCodeGroupCode : Pickler[SetReqCodeGroupCode]  = pickleCaseClass
  implicit val pickleSetReqCodeGroupTitle: Pickler[SetReqCodeGroupTitle] = pickleCaseClass
  implicit val pickleSetGenericReqTitle  : Pickler[SetGenericReqTitle]   = pickleCaseClass
  implicit val pickleSetCustomTextField  : Pickler[SetCustomTextField]   = pickleCaseClass
  implicit val pickleContentUpdate       : Pickler[ContentUpdate]        = pickleADT

  object Fn extends RemoteFn.ToVE[ContentUpdate]
}
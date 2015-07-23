package shipreq.webapp.base.protocol

import shipreq.base.util.{UnivEq, Position => Pos}
import shipreq.webapp.base.data._
import shipreq.webapp.base.event.DeletionAction
import shipreq.webapp.base.util.TypeclassDerivation._
import boopickle._, BoopickleMacros._, BinCodecGeneric._, BinCodecData._, BinCodecEvents._
import Field.ApplicableReqTypes

object FieldCrud {

  sealed trait Values

  case class TextFieldValues(name     : String,
                             key      : FieldRefKey,
                             mandatory: Mandatory,
                             reqTypes : ApplicableReqTypes) extends Values

  case class TagFieldValues(tagId    : TagId,
                            mandatory: Mandatory,
                            reqTypes : ApplicableReqTypes) extends Values

  case class ImplicationFieldValues(reqTypeId: ReqTypeId,
                                    mandatory: Mandatory,
                                    reqTypes : ApplicableReqTypes) extends Values

  implicit val equalText       : UnivEq[TextFieldValues]        = deriveUnivEq
  implicit val equalTag        : UnivEq[TagFieldValues]         = deriveUnivEq
  implicit val equalImplication: UnivEq[ImplicationFieldValues] = deriveUnivEq
  implicit def equalValues     : UnivEq[Values]                 = deriveUnivEq

  implicit val pickleFieldProtocolValues: Pickler[Values] = {
    implicit val pText        = pickleCaseClass[TextFieldValues]
    implicit val pTag         = pickleCaseClass[TagFieldValues]
    implicit val pImplication = pickleCaseClass[ImplicationFieldValues]
    pickleADT
  }

  // ===================================================================================================================

  type Position = Pos[FieldId]

  sealed trait CfgAction
  object CfgAction {
    final case class Create      (newValues: Values)                    extends CfgAction
    final case class UpdateValues(id: CustomFieldId, newValues: Values) extends CfgAction
    final case class UpdateOrder (id: FieldId, newPos: Position)        extends CfgAction
    final case class Delete      (id: FieldId, action: DeletionAction)  extends CfgAction

    implicit lazy val equality: UnivEq[CfgAction] = {import AutoDerive._; deriveUnivEq}

    implicit val pickleFieldProtocolCfgAction: Pickler[CfgAction] = {
      implicit val pCreate       = pickleCaseClass[Create]
      implicit val pUpdateValues = pickleCaseClass[UpdateValues]
      implicit val pUpdateOrder  = pickleCaseClass[UpdateOrder]
      implicit val pDelete       = pickleCaseClass[Delete]
      pickleADT
    }
  }

  // ===================================================================================================================

  object Fn extends RemoteFn.ToVE[CfgAction]
}
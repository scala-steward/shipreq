package shipreq.webapp.base.protocol

import scalaz.\&/
import shipreq.webapp.base.data._
import shipreq.webapp.base.delta.RemoteDelta
import shipreq.webapp.base.event.VerifiedEvents
import RemoteFn._

import BinCodecGeneric._
import BinCodecData._
import BinCodecProtocolData._
import BinCodecDelta._
import BinEventCodecs._

object RemoteFns {
  // After adding a new Routine, also update the following:
  // - ProtocolRemoteCodecs
  // - RandomData


  object ProjectInit extends (Unit =>|=> Project)

  // Project config
  object CustomIssueTypeCrud   extends Crudable.CAux[CustomIssueTypeId, CustomIssueTypeProtocol.Values]
  object CustomReqTypeCrud     extends Crudable.CAux[CustomReqTypeId,   CustomReqTypeProtocol.Values]
  object TagCrud               extends Crudable.CAux[TagId,             TagProtocol.Values \&/ TagProtocol.PovRelations]
  object FieldCrud             extends (FieldProtocol.CfgAction                =>|=> RemoteDelta)
  object FieldMandatorinessMod extends ((CustomFieldId,   Mandatory          ) =>|=> RemoteDelta)
  object ReqTypeImplicationMod extends ((CustomReqTypeId, ImplicationRequired) =>|=> VerifiedEvents)

  object UpdateProjectContent extends (ContentUpdate =>|=> RemoteDelta)


  case class ProjectSPA(projectInit:   ProjectInit          .Instance,
                        issueTypeCrud: CustomIssueTypeCrud  .Instance,
                        reqTypeCrud:   CustomReqTypeCrud    .Instance,
                        reqTypeImpMod: ReqTypeImplicationMod.Instance,
                        fieldMandMod:  FieldMandatorinessMod.Instance,
                        fieldCrud:     FieldCrud            .Instance,
                        tagCrud:       TagCrud              .Instance,
                        updateContent: UpdateProjectContent .Instance)
}
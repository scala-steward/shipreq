package shipreq.webapp.base.protocol

import scalaz.\&/
import shipreq.webapp.base.data._
import shipreq.webapp.base.delta.RemoteDelta
import Routine._

import upickle.TupleCodecs._
import GenericCodecs._
import DataCodecs._
import ProtocolDataCodecs._
import DeltaCodecs._

object Routines {

  object ProjectInit extends (Unit =>|=> Project)

  // Project config
  object CustomIssueTypeCrud   extends Crudable.CAux[CustomIssueTypeId, CustomIssueTypeProtocol.Values]
  object CustomReqTypeCrud     extends Crudable.CAux[CustomReqTypeId,   CustomReqTypeProtocol.Values]
  object TagCrud               extends Crudable.CAux[TagId,             TagProtocol.Values \&/ TagProtocol.PovRelations]
  object FieldCrud             extends (FieldProtocol.CfgAction                =>|=> RemoteDelta)
  object FieldMandatorinessMod extends ((CustomFieldId,   Mandatory          ) =>|=> RemoteDelta)
  object ReqTypeImplicationMod extends ((CustomReqTypeId, ImplicationRequired) =>|=> RemoteDelta)


  case class ProjectSPA(projectInit:   ProjectInit          .Remote,
                        issueTypeCrud: CustomIssueTypeCrud  .Remote,
                        reqTypeCrud:   CustomReqTypeCrud    .Remote,
                        reqTypeImpMod: ReqTypeImplicationMod.Remote,
                        fieldMandMod:  FieldMandatorinessMod.Remote,
                        fieldCrud:     FieldCrud            .Remote,
                        tagCrud:       TagCrud              .Remote)
}
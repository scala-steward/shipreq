package shipreq.webapp.base.protocol

import shipreq.webapp.base.data._
import BinCodecGeneric._
import BinCodecData._

object ProjectInit extends (Unit =>|=> Project)

object CustomIssueTypeCrud extends Crudable.CAux[CustomIssueTypeId, (HashRefKey, Option[String])]

object CustomReqTypeCrud extends Crudable.CAux[CustomReqTypeId, (ReqType.Mnemonic, String, ImplicationRequired)]

object FieldMandatorinessMod extends RemoteFn.ToVE[(CustomFieldId, Mandatory)]

object ReqTypeImplicationMod extends RemoteFn.ToVE[(CustomReqTypeId, ImplicationRequired)]

case class ProjectSPA(projectInit:   ProjectInit          .Instance,
                      issueTypeCrud: CustomIssueTypeCrud  .Instance,
                      reqTypeCrud:   CustomReqTypeCrud    .Instance,
                      reqTypeImpMod: ReqTypeImplicationMod.Instance,
                      fieldMandMod:  FieldMandatorinessMod.Instance,
                      fieldCrud:     FieldCrud            .Fn.Instance,
                      tagCrud:       TagCrud              .Fn.Instance,
                      updateContent: ContentUpdate        .Fn.Instance)
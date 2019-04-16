package shipreq.webapp.base.protocol2

import boopickle.Pickler
import scalaz.\/
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.data._
import shipreq.webapp.base.event.VerifiedEvent
import shipreq.webapp.base.Urls
import shipreq.webapp.base.protocol._
import BinCodecGeneric._
import BinCodecMemberData._
import BinCodecUser._
import BinCodecEvents._

object MemberProtocols {

  private def ajax[Req: Pickler, Res: Pickler](path: String): Protocol.Ajax.Simple[Pickler, Req, Res] =
    Protocol.Ajax.Simple(Urls.ajaxRoot / path, Protocol(implicitly), Protocol(implicitly))

  type ErrOrEvents = ErrorMsg \/ VerifiedEvent.Seq

  lazy val createProject         = ajax[String                                , ProjectMetaData]("p/c")
  lazy val projectNameSet        = ajax[String                                , ErrOrEvents    ]("pe/n")
  lazy val fieldMandatorinessMod = ajax[(CustomFieldId, Mandatory)            , ErrOrEvents    ]("pe/fm")
  lazy val reqTypeImplicationMod = ajax[(CustomReqTypeId, ImplicationRequired), ErrOrEvents    ]("pe/rti")
  lazy val createContent         = ajax[CreateContentCmd                      , ErrOrEvents    ]("pe/cc")
  lazy val updateContent         = ajax[UpdateContentCmd                      , ErrOrEvents    ]("pe/cu")
  lazy val updateSavedViews      = ajax[SavedViewCmd                          , ErrOrEvents    ]("pe/sv")

//  lazy val customIssueTypeCrud   = CrudProtocol[CustomIssueTypeId, (HashRefKey, Option[String])]("Project.CustomIssueTypeCrud")
//  lazy val customReqTypeCrud     = CrudProtocol[CustomReqTypeId, (ReqType.Mnemonic, String, ImplicationRequired)]("Project.CustomReqTypeCrud")

}

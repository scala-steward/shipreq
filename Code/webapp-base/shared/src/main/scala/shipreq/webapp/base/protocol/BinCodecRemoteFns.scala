package shipreq.webapp.base.protocol

import boopickle._
import BoopickleMacros._
import BinCodecGeneric._

object BinCodecRemoteFns {

  private def pickleRemoteFn(fn: RemoteFn): Pickler[fn.Instance] =
    xmap[fn.Instance, String](RemoteFn.Instance(_, fn))(_.key)

  implicit lazy val pickleProjectInit     = pickleRemoteFn(ProjectInit)
  implicit lazy val pickleIssueTypeCrud   = pickleRemoteFn(CustomIssueTypeCrud)
  implicit lazy val pickleReqTypeCrud     = pickleRemoteFn(CustomReqTypeCrud)
  implicit lazy val pickleReqTypeImpMod   = pickleRemoteFn(ReqTypeImplicationMod)
  implicit lazy val pickleFieldMandMod    = pickleRemoteFn(FieldMandatorinessMod)
  implicit lazy val pickleFieldCrud       = pickleRemoteFn(FieldCrud.Fn)
  implicit lazy val pickleTagCrud         = pickleRemoteFn(TagCrud.Fn)
  implicit lazy val pickleCreateContentFn = pickleRemoteFn(CreateContentFn)
  implicit lazy val pickleUpdateContentFn = pickleRemoteFn(UpdateContentFn)
}

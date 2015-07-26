package shipreq.webapp.base.protocol

import boopickle._
import BoopickleMacros._
import BinCodecGeneric._

object BinCodecRemoteFns {

  private def pickleRemoteFn(fn: RemoteFn): Pickler[fn.Instance] =
    xmap[fn.Instance, String](RemoteFn.Instance(_, fn))(_.key)

  implicit final val pickleProjectInit          = pickleRemoteFn(ProjectInit)
  implicit final val pickleIssueTypeCrud        = pickleRemoteFn(CustomIssueTypeCrud)
  implicit final val pickleReqTypeCrud          = pickleRemoteFn(CustomReqTypeCrud)
  implicit final val pickleReqTypeImpMod        = pickleRemoteFn(ReqTypeImplicationMod)
  implicit final val pickleFieldMandMod         = pickleRemoteFn(FieldMandatorinessMod)
  implicit final val pickleFieldCrud            = pickleRemoteFn(FieldCrud.Fn)
  implicit final val pickleTagCrud              = pickleRemoteFn(TagCrud.Fn)
  implicit final val pickleUpdateProjectContent = pickleRemoteFn(UpdateContentFn)

  implicit final val pickleProjectSPA = pickleCaseClass[ProjectSPA]
}

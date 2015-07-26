package shipreq.webapp.base.protocol

import shipreq.base.util.UnivEq, UnivEq.Implicits._
import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import boopickle._, BoopickleMacros._, BinCodecData._, BinCodecEvents._

/**
 * A command to create new content in a Project.
 */
sealed trait CreateContentCmd
object CreateContentCmd {

  case class CreateGenericReq(rt: CustomReqTypeId, vs: CreateGenericReqGD.Values) extends CreateContentCmd
  case class CreateReqCodeGroup(vs: ReqCodeGroupGD.NonEmptyValues)                extends CreateContentCmd

  implicit val pickleCreateGenericReq  : Pickler[CreateGenericReq]   = pickleCaseClass
  implicit val pickleCreateReqCodeGroup: Pickler[CreateReqCodeGroup] = pickleCaseClass
  implicit val pickleCmd               : Pickler[CreateContentCmd]   = pickleADT
}

object CreateContentFn extends RemoteFn.ToVE[CreateContentCmd]

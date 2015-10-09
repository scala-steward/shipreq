package shipreq.benchmark.data

import scalaz.{\/, \/-, -\/}
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.Text

object Migration {

  implicit def idsAreNowIntsInsteadOfLongs(l: Long): Int = l.toInt

  case class OldReqCodeGroup(title: Text.ReqCodeGroupTitle.OptionalText)

  case class OldActiveData(id: ReqCodeId, target: OldReqCodeGroup \/ ReqId)

  def ReqCodeData(active: Option[OldActiveData],
                  lastGroup  : Option[OldReqCodeGroup],
                  oldGroups: Set[ReqCodeId],
                  reqInactive: ReqCode.ReqInactive): ReqCode.Data = {
    def dg: ReqCode.DeadGroup =
      if (oldGroups.isEmpty) None else
      lastGroup.map(o => DeadReqCodeGroup(oldGroups.head, o.title))
    active match {
      case Some(OldActiveData(id, \/-(reqId))) => ReqCode.ActiveReq(id, reqId, dg, reqInactive)
      case Some(OldActiveData(id, -\/(g))) => ReqCode.ActiveGroup(LiveReqCodeGroup(id, g.title), reqInactive)
      case None => ReqCode.Inactive(dg, reqInactive)
    }
  }

  def ReqCodeActiveData(id: ReqCodeId, target: OldReqCodeGroup): OldActiveData =
    OldActiveData(id, -\/(target))

  def ReqCodeActiveData(id: ReqCodeId, target: ReqId): OldActiveData =
    OldActiveData(id, \/-(target))
}

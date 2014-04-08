package shipreq.taskman.api.impl

import scala.slick.session.Session
import shipreq.taskman.api._
import Serialisation.Ser

private[api] class ApiSql(prefix: String) {
  import shipreq.base.db.SqlHelpers._
  import scala.slick.jdbc.{GetResult, SetParameter}
  import scala.slick.jdbc.StaticQuery.{query, update}
  
  implicit val GR_JsonMsg = GR_Json[Msg]
  implicit val SP_JsonMsg = SP_Json[Msg]
  implicit val GR_MsgId: GetResult[MsgId] = IGR[Long] andThen MsgId
  implicit val SP_MsgId: SetParameter[MsgId] = ISP[Long] contramap (_.value)

  // Matches on db enum: msg_status_v01
  implicit val GR_MsgStatus: GetResult[MsgStatus] = implicitly[GetResult[String]] andThen {
    case "unassigned"    => MsgStatus.Unassigned
    case "node_assigned" => MsgStatus.NodeAssigned
    case "working"       => MsgStatus.Working
    case "complete"      => MsgStatus.Complete
    case "aborted"       => MsgStatus.Aborted
  }

  val CreateMsg = query[(Short, Option[Ser], Short), MsgId](
    s"select ${prefix}create_msg_v01(?::int2, ?::json, ?::int2)")

  val CfgPut = update[(String, String)](
    s"select ${prefix}cfg_update(?::VARCHAR, ?::TEXT)")

  val QueryMsgStatus = query[MsgId, MsgStatus](
    s"select ${prefix}query_msg_status_v01(?)")
}

// =====================================================================================================================

private[api] class ApiDao(ctx: TaskmanApi.Context, session: Session) {
  import ctx.sql._

  implicit def _session = session

  def createMsg(m: Msg): MsgId =
    createMsg(MsgType lookup m, Serialisation serialise m, Priority of m)

  def createMsg(m: MsgType, taskData: Ser, p: Priority): MsgId =
    CreateMsg.first(m.id.toShort, Some(taskData), p.value)

  def cfgPut(k: String, v: String): Unit =
    CfgPut.execute(k, v)

  def queryMsgStatus(id: MsgId): Option[MsgStatus] =
    QueryMsgStatus.firstOption(id)
}

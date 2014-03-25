package shipreq.taskman.server

import org.joda.time.{DateTime, Period}
import org.postgresql.util.PGInterval
import scala.slick.session.{Database, Session}
import scalaz.~>
import scalaz.effect.IO
import shipreq.taskman.api.{Priority, Msg}
import Sop._

object SopImpl {

  object Sql {
    import java.sql.Timestamp
    import scala.slick.jdbc.{GetResult, SetParameter}
    import scala.slick.jdbc.StaticQuery.{query, queryNA, update, updateNA}
    import scala.slick.session.PositionedParameters
    import shipreq.base.db.SqlHelpers._

    implicit val GR_JsonMsg = GR_Json[Msg]
    implicit val SP_JsonMsg = SP_Json[Msg]

    // TODO joda time + slick should be shared in base-db
    implicit def TimestampToDateTime(t: Timestamp): DateTime = new DateTime(t)
    implicit val GR_DateTime = GetResult(r => TimestampToDateTime(r.nextTimestamp))

    implicit val GR_MsgId = GetResult(r => MsgId(r.<<))

    implicit object SP_NodeId extends SetParameter[NodeId] {
      def apply(v: NodeId, pp: PositionedParameters): Unit = pp setInt v.value
    }

    implicit val GR_Priority = GetResult(r => Priority(r.<<))
    implicit object SP_Priority extends SetParameter[Priority] {
      def apply(v: Priority, pp: PositionedParameters): Unit = pp setShort v.value
    }

    implicit object SP_Period extends SetParameter[Period] {
      def apply(v: Period, pp: PositionedParameters): Unit = {
        val i = new PGInterval(
          v.getYears, v.getMonths, v.getDays, v.getHours, v.getMinutes,
          v.getSeconds.toDouble + v.getMillis/1000.0
        )
        pp.setObject(i, java.sql.Types.OTHER)
      }
    }

    implicit val GR_MsgHeader = GetResult(r => MsgHeader(r.<<, r.<<, r.<<))

    private[this] def getMsgsAssignNode_q(extraSel: Option[String], extraCond: Option[String]) =
      s"""
           select ctid ${extraSel.map(s => s",$s") getOrElse ""}
           from msgq
           where
             effective_from <= now()
             and (
               node is null               -- Unassigned
               or updated_at <= now()-?   -- Assignment lapsed
             )
             ${extraCond.map(s => s"and($s)") getOrElse ""}
           order by priority desc
           limit ? -- for update
      """.sql

    private[this] def getMsgsAssignNode_upd(ctids: String) =
      s"""
         update msgq
         set node = ?, worker = NULL, updated_at = now()
         where ctid in ($ctids)
         returning id, priority, created_at
      """.sql

    val getMsgsAssignNodeZ = query[(NodeId, Period, Int), MsgHeader](
      getMsgsAssignNode_upd(getMsgsAssignNode_q(None, None)))

    val getMsgsAssignNodeF = query[(NodeId, Period, Priority, Int), MsgHeader](
      getMsgsAssignNode_upd(getMsgsAssignNode_q(None, Some("priority > ?"))))

    val getMsgsAssignNodeP = query[(Period, Int, Int, Priority, NodeId), MsgHeader](
      s"""
        with a as (${getMsgsAssignNode_q(Some("priority p"), None)})
        , b as (
            select ctid from a
            order by p desc
            limit greatest(?,(select count(1) from a where p>?))
        )
        ${getMsgsAssignNode_upd("select ctid from b")}
      """.sql)
  }

  // ===================================================================================================================

  class Dao(implicit session: Session) {
    import Sql._

    def getMsgsAssignNode(node: NodeId, limit: Int, assignmentTrustPeriod: Period, queued: Option[(Priority, Int)]): List[MsgHeader] =
      queued match {
        case None =>
          // Empty mem-queue
          getMsgsAssignNodeZ.list(node, assignmentTrustPeriod, limit)

        case Some((memPri, memSize)) =>
          val freeSlots = limit - memSize
          if (freeSlots > 0)
            // Partial mem-queue
            getMsgsAssignNodeP.list(assignmentTrustPeriod, limit, freeSlots, memPri, node)
          else
            // Full mem-queue
            getMsgsAssignNodeF.list(node, assignmentTrustPeriod, memPri, limit)
      }
  }
}

// =====================================================================================================================

class SopImpl(db: Database) extends (Sop ~> IO) {
  override def apply[A](op: Sop[A]): IO[A] = op match {

    case GetMsgsAssignNode(node, limit, trustPeriod, queued) =>
      IO{
        db.withSession(implicit s =>
          new SopImpl.Dao().getMsgsAssignNode(node, limit, trustPeriod, queued)
        )
      }

    /*
    case class GetMsgAssignWorker(n: NodeId, w: WorkerId, m: MsgHeader) extends Sop[Option[MsgDetail]]
    case class MarkMsgComplete(m: MsgDetail) extends Sop[Unit]
    case class MsgFailedAbort(m: MsgDetail) extends FailedJobReaction
    case class MsgFailedRetry(m: MsgDetail, p: Period) extends FailedJobReaction
    case class NotifySupportWorkerFailed(m: MsgDetail, e: Error) extends Sop[Unit]
    case class NotifySupportTaskmanError(e: Error, m: Option[MsgDetail]) extends Sop[Unit]
     */
  }
}

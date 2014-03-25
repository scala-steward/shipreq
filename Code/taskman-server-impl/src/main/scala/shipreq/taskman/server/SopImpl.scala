package shipreq.taskman.server

import org.joda.time.{DateTime, Period}
import org.postgresql.util.PGInterval
import scala.slick.session.{Database, Session}
import scalaz.~>
import scalaz.effect.IO
import shipreq.base.util.ErrorOr
import shipreq.taskman.api.{Priority, Msg}
import shipreq.taskman.api.Types._
import shipreq.taskman.api.impl.Serialisation
import Sop._

object SopImpl {

  sealed trait ArchiveIntent {
    def resultFlag: Char
    val resultFlagS = resultFlag.toString
    def incFailureCount_? : Boolean
    def failureCountInc = if (incFailureCount_?) 1 else 0
  }

  case object Succeeded extends ArchiveIntent {
    override def resultFlag = 's'
    override def incFailureCount_? = false
  }

  case object FailAndAbort extends ArchiveIntent {
    override def resultFlag = 'f'
    override def incFailureCount_? = true
  }

  object Sql {
    import java.sql.Timestamp
    import scala.slick.jdbc.{GetResult, SetParameter}
    import scala.slick.jdbc.StaticQuery.{query, update}
    import scala.slick.session.PositionedParameters
    import shipreq.base.db.SqlHelpers._

    implicit val GR_JsonMsg = GR_Json[Msg]
    implicit val SP_JsonMsg = SP_Json[Msg]

    // TODO joda time + slick should be shared in base-db
    implicit def TimestampToDateTime(t: Timestamp): DateTime = new DateTime(t)
    implicit val GR_DateTime = GetResult(r => TimestampToDateTime(r.nextTimestamp))

    implicit object SP_ArchiveIntent extends SetParameter[ArchiveIntent] {
      def apply(v: ArchiveIntent, pp: PositionedParameters): Unit = {
        pp setString v.resultFlagS
        pp setInt v.failureCountInc
      }
    }

    implicit val GR_MsgId = GetResult(r => MsgId(r.<<))
    implicit object SP_MsgId extends SetParameter[MsgId] {
      def apply(v: MsgId, pp: PositionedParameters): Unit = pp setLong v.value
    }

    implicit object SP_NodeId extends SetParameter[NodeId] {
      def apply(v: NodeId, pp: PositionedParameters): Unit = pp setInt v.value
    }
    implicit object SP_NodeIdO extends SetParameter[Option[NodeId]] {
      def apply(v: Option[NodeId], pp: PositionedParameters): Unit = pp setIntOption v.map(_.value)
    }

    implicit object SP_WorkerId extends SetParameter[WorkerId] {
      def apply(v: WorkerId, pp: PositionedParameters): Unit = pp setShort v.value
    }
    implicit object SP_WorkerIdO extends SetParameter[Option[WorkerId]] {
      def apply(v: Option[WorkerId], pp: PositionedParameters): Unit = pp setShortOption v.map(_.value)
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

    private[this] def getMsgsAssignNode_q(extraSel: Option[String], extraCond: Option[String]) = s"""
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

    private[this] def getMsgsAssignNode_upd(ctids: String) = s"""
         update msgq
         set node = ?, worker = NULL, updated_at = now()
         where ctid in ($ctids)
         returning id, priority, created_at
      """.sql

    val getMsgsAssignNodeZ = query[(NodeId, Period, Int), MsgHeader](
      getMsgsAssignNode_upd(getMsgsAssignNode_q(None, None)))

    val getMsgsAssignNodeF = query[(NodeId, Period, Priority, Int), MsgHeader](
      getMsgsAssignNode_upd(getMsgsAssignNode_q(None, Some("priority > ?"))))

    val getMsgsAssignNodeP = query[(Period, Int, Int, Priority, NodeId), MsgHeader](s"""
        with a as (${getMsgsAssignNode_q(Some("priority p"), None)})
        , b as (
            select ctid from a
            order by p desc
            limit greatest(?,(select count(1) from a where p>?))
        )
        ${getMsgsAssignNode_upd("select ctid from b")}
      """.sql)

    val getMsgAssignWorkerQ = query[(WorkerId, MsgId, NodeId), (Short, Json[Msg], Short)]("""
        update msgq
        set worker = ?, updated_at = now()
        where id = ? and node = ? and worker is null
        returning type, data, failure_count
      """.sql)

    // TODO Doesn't confirm worker. and node = ? and worker = ?  , NodeId, WorkerId
    val failAndRetryQ = update[(Period, MsgId)]("""
        update msgq
        set
          node = null,
          worker = null,
          failure_count = failure_count + 1,
          updated_at = now(),
          effective_from = now() + ?
        where id = ?
      """.sql)

    // TODO Doesn't confirm worker. and node = ? and worker = ?  , NodeId, WorkerId
    val archiveMsgQ = update[(MsgId, ArchiveIntent)]("""
        with tmp as (
          delete from msgq where id=?
          returning id, type, data, ?, failure_count+?, created_at, now()
        )
        insert into msg_history select * from tmp
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

    def getMsgAssignWorker(node: NodeId, worker: WorkerId, hdr: MsgHeader): Option[MsgDetail] =
      getMsgAssignWorkerQ.firstOption(worker, hdr.id, node) map {
        case (msgType, msgData, failureCount) =>
          ErrorOr.require_!(
            Serialisation.deserialise(msgType, msgData).map(msg =>
              MsgDetail(hdr, msg, failureCount)))
      }

    def failAndRetry(msg: MsgId, delay: Period): Unit =
      failAndRetryQ.execute(delay, msg)
    
    def archiveMsg(msg: MsgId, status: ArchiveIntent): Unit =
      archiveMsgQ.execute(msg, status)

  }
}

// =====================================================================================================================

class SopImpl(db: Database) extends (Sop ~> IO) {
  import SopImpl._

  private[this] def io[A](f: Dao => A): IO[A] =
    IO(db.withSession(implicit s => f(new Dao())))

  override def apply[A](op: Sop[A]): IO[A] = op match {

    case GetMsgsAssignNode(node, limit, trustPeriod, queued) =>
      io(_.getMsgsAssignNode(node, limit, trustPeriod, queued))

    case GetMsgAssignWorker(node, worker, hdr) =>
      io(_.getMsgAssignWorker(node, worker, hdr))

    case MsgFailedRetry(m, delay) =>
      io(_.failAndRetry(m, delay))

    case MarkMsgComplete(m) =>
      io(_.archiveMsg(m, Succeeded))

    case MsgFailedAbort(m) =>
      io(_.archiveMsg(m, FailAndAbort))

    /*
    case class NotifySupportWorkerFailed(m: MsgDetail, e: Error) extends Sop[Unit]
    case class NotifySupportTaskmanError(e: Error, m: Option[MsgDetail]) extends Sop[Unit]
     */
  }
}

package shipreq.taskman.server.akka

import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import java.util.concurrent.atomic.AtomicInteger
import org.joda.time.Period
import scala.concurrent.duration._
import shipreq.taskman.api.Priority
import shipreq.taskman.server.{Worker, TaskmanCtx, WorkerId, MsgHeader}

object SourceActor {
  def props(ctx: TaskmanCtx) = Props(classOf[SourceActor], ctx)

  case class RequestForWork(queueStatus: Option[(Priority, Int)])
  case class IncomingWork(work: Seq[MsgHeader])
}

class SourceActor(ctx: TaskmanCtx) extends Actor with ActorLogging {
  import SourceActor._
  import shipreq.taskman.server.{Source => S}
  import ctx._

  // TODO HARDCODED values
  val source = S.Reified(Period seconds 1, manager.queueSize, manager.trustPeriod)
  var state: S.S = source.empty.unsafePerformIO()

  override def receive = {
    case RequestForWork(qs) =>
      val (s2, ms) = source.poll(qs).run(state).unsafePerformIO()
      state = s2
      if (ms.nonEmpty)
        sender() ! IncomingWork(ms)
  }
}

// =====================================================================================================================

// TODO TaskmanCtx unneeded
object ManagerActor {
  def props(ctx: TaskmanCtx, source: ActorRef) = Props(classOf[ManagerActor], ctx, source)

  case object PollSource extends Serializable
  case object RegisterWorker
  case object WorkAvailable
  case object RequestForWork
}

class ManagerActor(ctx: TaskmanCtx, source: ActorRef) extends Actor with ActorLogging {
  import ManagerActor._
  import shipreq.taskman.server.{Manager => M}
  import context.dispatcher

  var workers: Set[ActorRef] = Set.empty
  var queue = M.emptyQueue

  // TODO HARDCODED values
  val poller = context.system.scheduler.schedule(500 millis, 2000 millis, self, PollSource)

  override def postStop() = poller.cancel()

  override def receive = {

    case PollSource =>
      val qs = M.getQueueStatus.eval(queue)
      source ! SourceActor.RequestForWork(qs)

    case RegisterWorker =>
      workers += sender()
      log.debug("{} registered workers.", workers.size)

    case SourceActor.IncomingWork(work) =>
      queue = M.addToQueue(work).exec(queue)
      workers foreach (_ ! WorkAvailable)

    case RequestForWork =>
      val (q2, wo) = M.popJob.run(queue)
      wo foreach (sender() ! _)
      queue = q2
  }
}

// =====================================================================================================================

object WorkerActor {
  def props(ctx: TaskmanCtx, manager: ActorRef) = Props(classOf[WorkerActor], ctx, manager)

  private[this] val idCounter = new AtomicInteger
  def nextId(): WorkerId = WorkerId(idCounter.incrementAndGet().toShort)
}

class WorkerActor(ctx: TaskmanCtx, manager: ActorRef) extends Actor with ActorLogging {
  import ctx._
  import ManagerActor.{RequestForWork, WorkAvailable}

  implicit val id: WorkerId = WorkerActor.nextId
  val worker = Worker.Reified()

  private def requestWork(): Unit =
    manager ! RequestForWork

  override def receive = {

    case WorkAvailable =>
      requestWork()

    case m: MsgHeader =>
      worker.processL(m).unsafePerformIO()
      requestWork()
  }
}

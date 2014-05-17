package shipreq.taskman.server

import scalaz.{Heap, State, StateT}
import scalaz.effect.IO

object Manager {

  type JobQueue       = Heap[MsgHeader]
  type JobQueueS[A]   = State[JobQueue, A]
  type JobQueueSIO[A] = StateT[IO, JobQueue, A]

  implicit object PrioritisationOrder extends Ordering[MsgHeader] {
    override def compare(x: MsgHeader, y: MsgHeader): Int = {
      val a = y.priority.value - x.priority.value
      if (a != 0) a else {
        val b = x.created.compareTo(y.created)
        if (b != 0) b else
          Ordering.Long.compare(x.id.value, y.id.value)
      }
    }
  }

  implicit val PrioritisationOrderZ = scalaz.Order.fromScalaOrdering[MsgHeader]

  def emptyQueue: JobQueue = Heap.Empty[MsgHeader]

  def addToQueue(ms: Seq[MsgHeader]): JobQueueS[Unit] =
    State.modify(_ insertAll ms)

  val getQueueStatus: JobQueueS[Source.QueueStatus] = // TODO cache?
    State.gets(q =>
      q.minimumO.map(m => (m.priority, q.size)))

  val popJob: JobQueueS[Option[MsgHeader]] =
    State(q =>
      q.minimumO match {
        case None      => (q, None)
        case m@Some(_) => (q.deleteMin, m)
      }
    )
}

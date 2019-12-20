package shipreq.taskman.server.logic

import scalaz.{Heap, State}

object Manager {

  final case class JobQueue(q: Heap[TaskHeader]) {
    def size: Int =
      q.size

    lazy val status: Source.QueueStatus =
      q.minimumO.map(m => (m.priority, q.size))
  }

  type JobQueueS[A] = State[JobQueue, A]

  implicit object PrioritisationOrder extends Ordering[TaskHeader] {
    override def compare(x: TaskHeader, y: TaskHeader): Int = {
      val a = y.priority.value - x.priority.value
      if (a != 0) a else {
        val b = x.created.compareTo(y.created)
        if (b != 0) b else
          Ordering.Long.compare(x.id.value, y.id.value)
      }
    }
  }

  implicit val PrioritisationOrderZ: scalaz.Order[TaskHeader] =
    scalaz.Order.fromScalaOrdering[TaskHeader]

  def empty: JobQueue =
    JobQueue(Heap.Empty[TaskHeader])

  def add(ms: Seq[TaskHeader]): JobQueueS[Unit] =
    State.modify(j => JobQueue(j.q insertAll ms))

  val pop: JobQueueS[Option[TaskHeader]] =
    State(j =>
      j.q.minimumO match {
        case None      => (j, None)
        case m@Some(_) => (JobQueue(j.q.deleteMin), m)
      }
    )
}

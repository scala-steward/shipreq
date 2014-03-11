package shipreq.taskman.api

import scalaz._
import Scalaz._
import scalaz.Free.{Gosub, Return, Suspend}
import scalaz.effect.IO

object Task1 {
  trait TaskDef
  sealed trait Task[R]

  case class Create[R](t: Seq[TaskDef], k: () => R) extends Task[R]

  type FreeTask[A] = Free[Task, A]

  val ReturnUnit: FreeTask[Unit] = Return(())
  val _ReturnUnit = () => ReturnUnit

  def create(t: Seq[TaskDef]): FreeTask[Unit] =
    Suspend(Create(t, _ReturnUnit))

  // -------------------------------------------------------------------------------------------------------------------

  val taskDef: TaskDef = new TaskDef {override def toString = "TASKDEF"}
  val freeTask: Free[Task, Unit] = Task1.create(Seq(taskDef)) >>= (_ => Task1.create(Seq(taskDef, taskDef)))

    @annotation.tailrec
  def runTask[A](io: FreeTask[A]): A = io match {
    case Return(a) => a
    case Suspend(Create(t, k)) => {println(t); runTask(k())}
    case g@Gosub(_, _) => {val a = handleGosub(g); runTask(g.f(a))} // uses stack
//    case g@Gosub(_, f) => runTask(f(handleGosub(g)))
//    case Gosub(a, f) => runTask(f(runTask(a())))
  }

  private def handleGosub[A](g: Gosub[Task, A, _]): A = runTask(g.a())

  implicit object TaskFunctor extends Functor[Task] {
    override def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa match {
      case Create(t, k) => Create(t, () => f(k()))
    }
  }

  object TaskIO extends (Task ~> IO) {
    override def apply[A](m: Task[A]): IO[A] = m match {
      case Create(t, k) =>
        IO {println(s"Creating: $t"); k() }
    }
  }
  val freeio: Free[IO, Unit] = freeTask.mapSuspension(TaskIO)

  object TaskF0 extends (Task ~> Function0) {
    override def apply[A](m: Task[A]): Function0[A] = m match {
      case Create(t, k) =>
        () => {println(s"Creating: $t"); k() }
    }
  }

  def main(args: Array[String]): Unit = {
    val freeF0: Free[Function0, Unit] = freeTask.mapSuspension(TaskF0)
    freeF0.run
  }

//  val freeio2 = freeio.

}
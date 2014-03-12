package shipreq.taskman.api

import scalaz.{Coyoneda, Free, ~>, Functor, ReaderT, Kleisli}
import scalaz.Scalaz.Id
import scalaz.effect.IO
import scalaz.Free.FreeC
import scalaz.std.function.function0Instance

import ScalazExt._

/*
// Merge with webapp's types

object Types extends Types {
  sealed trait IsUserId extends TypeTag[JLong]
}

trait Types extends TypeTags {
  import Types._
  type UserId = JLong @@ IsUserId
}
*/


sealed trait TaskDef

object TaskDef {
  case class UserAttemptsRegistration(email: String, url: Option[String]) extends TaskDef
}

object TaskmanApi {

  trait Cmd[A]
  type CmdF[A] = FreeC[Cmd, A]
  implicit def cmdLiftF[A](c: Cmd[A]): CmdF[A] = liftFC(c)

  case class SubmitTask1(w: TaskDef) extends Cmd[Unit]
  case class SubmitTask(w: Seq[TaskDef]) extends Cmd[Unit]
  case object CountTasks extends Cmd[Long] // TODO Delete. Is sample.
}

object Effect {

  type IOM[A] = Function0[A]

  def iom[A](a: => A): IOM[A] = () => a

  def compile[C[_], A](f: FreeC[C, A], t: C ~> IOM): IO[A] = {
    val g = f.mapSuspension(FG_to_CFG(t))
    IO{ g.run }
  }
}

object TaskmanApiImpl {
  trait DatabaseHandle {
    def submit(blah: Any): Unit
    def count: Long
  }

  import TaskmanApi._
  import Effect._

  def reify(db: DatabaseHandle): (Cmd ~> IOM) =
    new (Cmd ~> IOM) {
      def apply[A](c: Cmd[A]): IOM[A] = c match {
        case SubmitTask1(w) => iom { db submit w }
        case SubmitTask(ws) => iom { ws.foreach(db.submit(_)) }
        case CountTasks     => iom { db.count }
      }
    }

  def usage(): Unit = {
    val cmd = SubmitTask1(TaskDef.UserAttemptsRegistration("", None))
    val program = cmdLiftF(cmd)
    val db: DatabaseHandle = ???
    val io = compile(program, reify(db))
    io.unsafePerformIO()
  }
}
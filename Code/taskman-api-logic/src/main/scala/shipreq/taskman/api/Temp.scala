package shipreq.taskman.api

import scalaz.{Coyoneda, Free, ~>, Functor, \/, -\/}
import scalaz.Scalaz.Id
import scalaz.effect.IO
import scalaz.Free.FreeC
import scalaz.std.function.function0Instance

import ScalazExt._
import shipreq.base.util.TypeTags
//  import scalaz._, Scalaz._
//  import scalaz.syntax.functor._
//  import scalaz.std.list._

// TODO Merge with webapp's types

object Types extends Types {
  sealed trait IsUserId extends TypeTag[JLong]
  sealed trait IsEmailAddr extends TypeTag[String]
}

trait Types extends TypeTags {
  import Types._
  type UserId = JLong @@ IsUserId
  type EmailAddr = String @@ IsEmailAddr
}
import Types._

// --------------------------------------------------------------------------------------------------------------------

object TaskTypes {
  val AsList: List[TaskType] = TaskType.values.toList

  val ById: Map[Int, TaskType] = {
    val groups = AsList.groupBy(_.id)
    val dupGroups = groups.filterNot(_._2.size == 1)
    if (dupGroups.nonEmpty)
      throw new ExceptionInInitializerError(s"${classOf[TaskType].getSimpleName} with duplicate IDs found: $dupGroups")
    groups.mapValues(_.head).toMap
  }

  import java.util.EnumMap

  private val ToDef: EnumMap[TaskType, Class[_ <: TaskDef]] = {
    import shipreq.taskman.api.{TaskType => T}
    import shipreq.taskman.api.{TaskDef => D}

    def x(tt: T): Class[_ <: TaskDef] = tt match {
      case T.RegistrationRequested  => classOf[D.RegistrationRequested]
      case T.RegistrationCompleted  => classOf[D.RegistrationCompleted]
      case T.PasswordResetRequested => classOf[D.PasswordResetRequested]
      case T.LandingPageHit         => classOf[D.LandingPageHit]
    }

    val m = new EnumMap[TaskType, Class[_ <: TaskDef]](classOf[TaskType])
    for (tt <- TaskType.values) m.put(tt, x(tt))
    m
  }
  private val FromDef: Map[Class[_ <: TaskDef], TaskType] =
    TaskType.values.map(t => (defClass(t) -> t)).toMap

  def defClass(t: TaskType): Class[_ <: TaskDef] = ToDef.get(t)

  def forDef(d: TaskDef): TaskType = FromDef(d.getClass)
}

/*
sealed abstract class TaskDef(final val typ: TaskType) {
  @inline final def id = typ.id
}
object TaskDef {
  import shipreq.taskman.api.{TaskType => T}

  case class RegistrationRequested(email: EmailAddr, url: Option[String])
    extends TaskDef(T.RegistrationRequested)

  case class RegistrationCompleted(userId: UserId)
    extends TaskDef(T.RegistrationCompleted)

  case class PasswordResetRequested(email: EmailAddr, url: String)
    extends TaskDef(T.PasswordResetRequested)

  case class LandingPageHit(email: EmailAddr, name: String, msg: Option[String], newsletter: Boolean)
    extends TaskDef(T.LandingPageHit)

  // UserChangedPrefs
  // MailChimpBroadcast
}
*/


sealed trait TaskDef
object TaskDef {
  case class RegistrationRequested(email: EmailAddr, url: Option[String]) extends TaskDef
  case class RegistrationCompleted(userId: UserId) extends TaskDef
  case class PasswordResetRequested(email: EmailAddr, url: String) extends TaskDef
  case class LandingPageHit(email: EmailAddr, name: String, msg: Option[String], newsletter: Boolean) extends TaskDef
  // UserChangedPrefs
  // MailChimpBroadcast
}

//object Main{
//  def main(a: Array[String]) {
//    println(TaskTypes.ById(102))
//  }
//}

// --------------------------------------------------------------------------------------------------------------------

object TaskmanApi {

  trait Cmd[A]
  type CmdF[A] = FreeC[Cmd, A]
  implicit def cmdLiftF[A](c: Cmd[A]): CmdF[A] = liftFC(c)

  case class SubmitTask1(w: TaskDef) extends Cmd[Unit]
  case class SubmitTask(w: Seq[TaskDef]) extends Cmd[Unit]
}

object Effect {

  type IOM[A] = Function0[A]

  def iom[A](a: => A): IOM[A] = () => a

  def compile[C[_], A](f: FreeC[C, A], t: C ~> IOM): IO[A] = {
    val g = f.mapSuspension(FG_to_CFG(t))
    IO{ g.run }
  }
}

object Serialisation {
  type Ser = Json[_]
  type Deser = String \/ TaskDef

  def ser(t: TaskDef): Ser = ??? // json4s blah blah

  def deser(taskTypeId: Int, s: Ser): Deser = {
    TaskTypes.ById.get(taskTypeId) match {
      case Some(tt) =>
        val defClass = TaskTypes.defClass(tt)
        ???  // json4s blah blah
      case None =>
        -\/(s"Unknown task type: $taskTypeId")
    }
  }
}

object TaskmanApiImpl {
  trait DatabaseHandle {
    def submit(t: TaskDef): Unit = {
      val n = TaskTypes.forDef(t).id
      val d = Serialisation.ser(t)
      submitSql(n, d)
    }
    def submitSql(id: Int, data: Json[_]): Unit
  }

  import TaskmanApi._
  import Effect._

  def reify(db: DatabaseHandle): (Cmd ~> IOM) =
    new (Cmd ~> IOM) {
      def apply[A](c: Cmd[A]): IOM[A] = c match {
        case SubmitTask1(w) => iom { db submit w }
        case SubmitTask(ws) => iom { ws.foreach(db.submit(_)) }
      }
    }

  def usage(): Unit = {
    val cmd = SubmitTask1(TaskDef.RegistrationRequested("a@b.com".tag, None))
    val program = cmdLiftF(cmd)
    val db: DatabaseHandle = ???
    val io = compile(program, reify(db))
    io.unsafePerformIO()
  }
}
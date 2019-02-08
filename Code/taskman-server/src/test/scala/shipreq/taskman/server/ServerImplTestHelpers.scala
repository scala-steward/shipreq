package shipreq.taskman.server

import java.util.concurrent.locks.ReentrantLock
import shipreq.base.test.db.{SingleConnectionXA, TestDb}
import shipreq.base.util.FxModule._
import shipreq.base.util.Props
import shipreq.taskman.api.TaskmanApi
import shipreq.taskman.server.logic.ServerOp
import ServerImplTestHelpers._

final case class ServerImplTestHelpers(xa: SingleConnectionXA) {

  lazy val ctx = TaskmanCtx(
    xa dbAccess TestDb.dbAccess,
    taskmanConfig,
    cfgSrc)

  lazy val taskmanApi = ctx.taskmanApi
  import ctx._

  def reify[A](op: ServerOp[A]): Fx[A] = serverOpFx(op)

  def runApi[A](f: TaskmanApi[Fx] => Fx[A]): A = f(taskmanApi).unsafeRun()
  def run[A](op: ServerOp[A]): A = reify(op).unsafeRun()
}

object ServerImplTestHelpers {
  private val mutex = Some(new ReentrantLock())

  private[server] def cfgSrc = Props.sources

  lazy val (taskmanConfig, taskmanConfigReport) =
    TaskmanConfig.config.withReport.run(cfgSrc).unsafeRun().getOrDie()

  def imperative(inTransaction: Boolean = true)(f: ServerImplTestHelpers => Any): Unit =
    TestDb(inTransaction, mutex)(xa => Fx(f(apply(xa)))).unsafeRun()
}
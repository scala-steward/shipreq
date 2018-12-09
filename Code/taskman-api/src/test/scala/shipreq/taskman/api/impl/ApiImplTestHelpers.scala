package shipreq.taskman.api.impl

import shipreq.base.test.db.{SingleConnectionXA, TestDb}
import shipreq.base.util.FxModule._
import shipreq.taskman.api.TaskmanApi

trait ApiImplTestHelpers {

  protected implicit def taskmanApi(xa: SingleConnectionXA): TaskmanApi[Fx] =
    TaskmanApiImpl(None).trans(xa.trans)

  def run[A](f: SingleConnectionXA => Fx[A]): A =
    TestDb()(xa => f(xa)).unsafeRun()

  def run_(ops: (TaskmanApi[Fx] => Fx[_])*): Unit =
    TestDb() { xa =>
      val api = taskmanApi(xa)
      ops.foldLeft(Fx.unit)( _ tap_ _(api))
    }.unsafeRun()

}

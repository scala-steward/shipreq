package shipreq.taskman.server

import java.util.concurrent.locks.ReentrantReadWriteLock
import scalaz.effect.IO
import scala.slick.session.Database
import shipreq.taskman.api.impl.TaskmanApi
import shipreq.taskman.api.ApiOp

trait ServerImplTestHelpers {
  def db: Database

  final def dbMutexR = ServerImplTestHelpers.dbMutexR
  final def dbMutexW = ServerImplTestHelpers.dbMutexW

  def apiOpReifier = new TaskmanApi(TaskmanApi.Context(None), db)
  def sopReifier = new SopImpl(db)

  def reify[A](op: ApiOp[A]): IO[A] = apiOpReifier(op)
  def reify[A](op: Sop[A]): IO[A] = sopReifier(op)

  def run[A](op: ApiOp[A]): A = reify(op).unsafePerformIO()
  def run[A](op: Sop[A]): A = reify(op).unsafePerformIO()
}

object ServerImplTestHelpers {

  val dbLockRW = new ReentrantReadWriteLock
  val dbMutexR = Some(dbLockRW.readLock)
  val dbMutexW = Some(dbLockRW.writeLock)

  def apply(_db: Database): ServerImplTestHelpers =
    new ServerImplTestHelpers {
      override def db = _db
    }
}
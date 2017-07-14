package shipreq.base.util

import java.util.concurrent.locks.Lock
import scalaz.effect.IO
import scalaz.syntax.bind.ToBindOps

object LockUtils {

  def inMutex[A](lock: Lock)(a: => A): A = {
    lock.lockInterruptibly()
    try a finally lock.unlock()
  }

  def maybeInMutex[A](mutex: Option[Lock])(a: => A): A =
    mutex.fold(a)(inMutex(_)(a))

  def inMutexIO[A](lock: Lock)(io: IO[A]): IO[A] =
    IO(lock.lockInterruptibly()) >> io.ensuring(IO(lock.unlock()))

  def maybeInMutexIO[A](mutex: Option[Lock])(io: IO[A]): IO[A] =
    mutex.fold(io)(inMutexIO(_)(io))

  import shipreq.base.util.FxModule._

  def inMutexFx[A](lock: Lock)(fx: Fx[A]): Fx[A] =
    Fx(lock.lockInterruptibly()) >> fx.ensuring(Fx(lock.unlock()))

  def maybeInMutexFx[A](mutex: Option[Lock])(fx: Fx[A]): Fx[A] =
    mutex.fold(fx)(inMutexFx(_)(fx))
}

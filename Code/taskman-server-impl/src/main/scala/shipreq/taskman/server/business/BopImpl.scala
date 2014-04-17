package shipreq.taskman.server.business

import scalaz.effect.IO
import scalaz.{-\/, \/-}
import shipreq.base.util.ErrorOr
import shipreq.base.util.effect.IOE
import shipreq.base.util.log.HasLogger
import shipreq.taskman.server.IoUtils
import Bop._
import BopImpl._

object BopImpl {
  trait Ctx {
    def emailer: EmailImpl
  }
}

final class BopImpl(ctx: Ctx) extends BopReifier with HasLogger {

  import ctx._

  override def apply[A](op: Bop[A]): IOE[A] =
    IoUtils.timeU(
      ErrorOr.catchExceptionM(applyOnly(op))
    )(logAfterWork(op))

  def logAfterWork[A](op: Bop[A]): ErrorOr[A] => Long => IO[Unit] =
    res => time => IO(
      res match {
        case \/-(_) =>
          log.info.z(s"${op.getClass.getSimpleName} completed in ${time}ms.")
        case -\/(e) =>
          log.error.z(s"${op.getClass.getSimpleName} failed after ${time}ms with [${e.msg}]. Op: $op")
      }
    )

  def applyOnly[A](op: Bop[A]): IOE[A] =
    op match {
      case s: SendEmail[EmailImpl.EA] => emailer send s
    }
}


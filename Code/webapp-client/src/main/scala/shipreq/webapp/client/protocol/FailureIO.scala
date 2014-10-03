package shipreq.webapp.client.protocol

import scalaz.effect.IO

final case class FailureIO(io: IO[Unit])

object FailureIO {
  def nop = FailureIO(IO(()))
}
package shipreq.webapp.client.lib

import scalaz.Bind
import scalaz.effect.IO
import shipreq.base.util.effect.IoUtils

final case class FailureIO(io: IO[Unit])

object FailureIO {

  def nop = FailureIO(IoUtils.nop)

  def lazily(f: => IO[Unit]): FailureIO = FailureIO(Bind[IO].join(IO(f)))
}
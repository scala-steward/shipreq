package shipreq.webapp.client.lib

import scalaz.Bind
import scalaz.effect.IO
import shipreq.base.util.effect.IoUtils

final case class SuccessIO(io: IO[Unit])

object SuccessIO {

  val nop = SuccessIO(IoUtils.nop)

  def lazily(f: => IO[Unit]): SuccessIO = SuccessIO(Bind[IO].join(IO(f)))
}
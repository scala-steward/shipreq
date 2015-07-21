package shipreq.webapp.client.lib

import scalaz.Bind
import scalaz.effect.IO
import shipreq.base.util.effect.IoUtils

final case class FailureIO(io: IO[Unit]) extends AnyVal {
  @inline def >>(next: IO[Unit]): FailureIO =
    FailureIO(io.flatMap(_ => next))

  @inline def <<(prev: IO[Unit]): FailureIO =
    FailureIO(prev.flatMap(_ => io))
}

object FailureIO {

  def nop = FailureIO(IoUtils.nop)

  def lazily(f: => IO[Unit]): FailureIO =
    FailureIO(Bind[IO].join(IO(f)))
}
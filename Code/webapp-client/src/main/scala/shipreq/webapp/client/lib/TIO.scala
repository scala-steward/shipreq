package shipreq.webapp.client.lib

import scalaz.Bind
import scalaz.effect.IO
import shipreq.base.util.effect.IoUtils

/**
 * Typed IO. An IO effect attached with type to indicate and distinguish purpose.
 */
final case class TIO[T <: TIO.Type](io: IO[Unit]) extends AnyVal {
  type Self = TIO[T]

  @inline def >>(next: IO[Unit]): Self =
    TIO(io.flatMap(_ => next))

  @inline def <<(prev: IO[Unit]): Self =
    TIO(prev.flatMap(_ => io))
}

object TIO {
  sealed trait Type

  final class Ctor[T <: Type] private[TIO]() {

    @inline def apply(io: IO[Unit]): TIO[T] =
      new TIO(io)

    @inline def nop =
      apply(IoUtils.nop)

    def lazily(f: => IO[Unit]) =
      apply(Bind[IO] join IO(f))
  }

  @inline implicit def autoResolveIo[T <: Type](cbio: TIO[T]): IO[Unit] =
    cbio.io

  // ----------------------------------
  // Types

  sealed trait TypeSuccess extends Type
  type Success = TIO[TypeSuccess]
  val Success = new Ctor[TypeSuccess]()

  sealed trait TypeFailure extends Type
  type Failure = TIO[TypeFailure]
  val Failure = new Ctor[TypeFailure]()
}
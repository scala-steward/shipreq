package shipreq.base.util

import scalaz.syntax.bind._
import scalaz.effect.IO

package object effect {

  // ===================================================================================================================
  // IO

  val nopIo: IO[Unit] = IO(())

  implicit class IOExt[A](val io: IO[A]) extends AnyVal {
    @inline def tap(f: A => IO[_]): IO[A] = io.flatMap(a => f(a) >> IO(a))
    @inline def <| (f: A => IO[_]): IO[A] = io tap f
  }

  // ===================================================================================================================
  // IOE

  type IOE[A] = IO[ErrorOr[A]]

  object IOE {
    def apply[A](f: => A): IOE[A] = IO(ErrorOr safe f)

    val nop = apply(())
  }

}

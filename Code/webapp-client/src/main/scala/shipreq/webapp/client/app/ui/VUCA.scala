package shipreq.webapp.client.app.ui

import scalaz.effect.IO
import shipreq.webapp.client.lib.TIO

/**
 * [V]alue
 * [U]pdate
 * [C]ommit
 * [A]bort
 */
case class VUCA[A, -B](value: A, update: A => IO[Unit], commit: B => TIO.Commit, abort: TIO.Abort)

object VUCA {

  /** Value & Update only. Abort & Commit will do nothing. */
  @inline def vu[A](value: A, update: A => IO[Unit]): VUCA[A, Any] =
    VUCA(value, update, TIO.Commit._nop, TIO.Abort.nop)
}
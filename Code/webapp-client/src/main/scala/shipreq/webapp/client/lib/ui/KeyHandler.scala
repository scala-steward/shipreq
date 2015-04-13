package shipreq.webapp.client.lib.ui

import japgolly.scalajs.react._, ScalazReact._
import shipreq.base.util.effect.IoUtils, IoUtils.IoExt
import scala.runtime.AbstractFunction1
import scalajs.js.{UndefOr, undefined}
import scalaz.effect.IO

final class KeyHandler(val run: ReactKeyboardEventH => UndefOr[IO[Unit]]) extends AbstractFunction1[ReactKeyboardEventH, IO[Unit]] {
  def |(b: KeyHandler): KeyHandler =
    new KeyHandler(e => run(e) orElse b.run(e))

  override def apply(e: ReactKeyboardEventH): IO[Unit] =
    run(e).fold(IoUtils.nop)(e.preventDefaultIO >>> e.stopPropagationIO >>> _)
}

object KeyHandler {
  def apply(f: ReactKeyboardEventH => UndefOr[IO[Unit]]): KeyHandler =
    new KeyHandler(f)

  private def reshapePF[A](pf: PartialFunction[A, UndefOr[IO[Unit]]]): A => UndefOr[IO[Unit]] =
    a => pf.applyOrElse(a, (_: A) => undefined)

  def pf(pf: PartialFunction[ReactKeyboardEventH, UndefOr[IO[Unit]]]): KeyHandler =
    new KeyHandler(reshapePF(pf))

  def by[A](f: ReactKeyboardEventH => A)(pf: PartialFunction[A, UndefOr[IO[Unit]]]): KeyHandler =
    new KeyHandler(reshapePF(pf) compose f)

  def modKeys(e    : ReactKeyboardEventH,
              alt  : Boolean = false,
              ctrl : Boolean = false,
              shift: Boolean = false,
              meta : Boolean = false): Boolean =
    e.altKey   == alt   &&
    e.ctrlKey  == ctrl  &&
    e.shiftKey == shift &&
    e.metaKey  == meta
}
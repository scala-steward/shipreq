package shipreq.base.util

import scala.scalajs.js.UndefOr

object PlatformExt {

  final class StreamUExt[A](val _a: Stream[UndefOr[A]]) extends AnyVal {
    def jsDefined: Stream[A] =
      _a.filter(_.isDefined).map(_.get)
  }

}

abstract class PlatformExt {

  import PlatformExt._

  final implicit def StreamUExt[A](s: Stream[UndefOr[A]]) =
    new StreamUExt[A](s)

}

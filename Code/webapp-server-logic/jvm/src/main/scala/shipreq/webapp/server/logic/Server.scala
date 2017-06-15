package shipreq.webapp.server.logic

import java.time.{Duration, Instant}
import shipreq.webapp.base.protocol.RemoteFn
import shipreq.base.util.ScalaExt._

object Server {

  trait Algebra[F[_]] {
    def remoteFn(fn: RemoteFn)(localFn: fn.Input => F[fn.Response]): F[fn.Instance]
    def now: F[Instant]
    def delay[A](f: F[A], d: Duration): F[A]
  }

  type Retries = List[Duration]

  def retriesFrom(d: Duration, factor: Double = 2): Stream[Duration] =
    d #:: retriesFrom((d.toMillis * factor).millis, factor)
}

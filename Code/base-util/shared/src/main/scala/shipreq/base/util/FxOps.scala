package shipreq.base.util

import java.time.{Duration, Instant}
import scalaz.Monad

trait FxOps[F[_]] {
  def apply[A](f: F[A]): FxOps.Syntax[F, A]
}

object FxOps {

  object Implicits {
    @inline implicit def autoFxOpSyntax[F[_], A](f: F[A])(implicit g: FxOps[F]): Syntax[F, A] =
      g(f)
  }

  trait Syntax[F[_], A] extends Any {

    def tap[B](f: A => F[B]): F[A]

    def tap_[B](f: F[B]): F[A] =
      tap(_ => f)

    def unsafeTap[B](f: A => B): F[A]

    def measureDuration: F[(A, Duration)]
  }

  def fromMonad[F[_]](implicit F: Monad[F]): FxOps[F] = {
    import scalaz.syntax.monad._
    val now = F.point(Instant.now())
    class MonadSyntax[A](private val fa: F[A]) extends Syntax[F, A] {
      override def tap[B](f: A => F[B]): F[A] =
        for {
          a <- fa
          _ <- f(a)
        } yield a
      override def unsafeTap[B](f: A => B): F[A] =
        tap(a => F.point(f(a)))
      override def measureDuration: F[(A, Duration)] =
        for {
          start <- now
          a     <- fa
          end   <- now
        } yield (a, Duration.between(start, end))
    }
    new FxOps[F] {
      override def apply[A](f: F[A]) = new MonadSyntax(f)
    }
  }

}
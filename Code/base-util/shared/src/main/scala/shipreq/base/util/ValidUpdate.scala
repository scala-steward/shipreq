package shipreq.base.util

import scalaz.{\/, -\/, \/-}
import ValidUpdate._

sealed abstract class ValidUpdate[+E, +A] {
  final def map[B](f: A => B): ValidUpdate[E, B] =
    flatMap(a => Success(f(a)))

  final def flatMap[B, EE >: E](f: A => ValidUpdate[EE, B]): ValidUpdate[EE, B] =
    this match {
      case Success(a)   => f(a)
      case x@Failure(_) => x
      case Unchanged    => Unchanged
    }

  final def mapFailure[F](f: E => F): ValidUpdate[F, A] =
    flatMapFailure(e => Failure(f(e)))

  final def flatMapFailure[F, AA >: A](f: E => ValidUpdate[F, AA]): ValidUpdate[F, AA] =
    this match {
      case x@Success(_) => x
      case Failure(e)   => f(e)
      case Unchanged    => Unchanged
    }
}

object ValidUpdate {

  case class Success[+A](updated: A) extends ValidUpdate[Nothing, A]

  case class Failure[+E](failure: E) extends ValidUpdate[E, Nothing]

  case object Unchanged extends ValidUpdate[Nothing, Nothing]

  def fromDisjunction[E, A](d: E \/ A)(unchanged: A => Boolean): ValidUpdate[E, A] =
    d match {
      case \/-(a) => if (unchanged(a)) Unchanged else Success(a)
      case -\/(e) => Failure(e)
    }
}
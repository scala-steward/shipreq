package shipreq.base.test.specs2

import org.specs2.matcher.StandardMatchResults._
import org.specs2.matcher.AnyMatchers._
import org.specs2.matcher.Matcher
import shipreq.base.util.ErrorOr
import scalaz.{\/-, -\/}

object BaseMatchers {

  def beAnError: Matcher[ErrorOr[_]] =
    beLike{ case -\/(_) => ok }

  def notBeAnError: Matcher[ErrorOr[_]] =
    beLike{ case \/-(_) => ok }

  def beNonErrorAnd[T](m: Matcher[T]): Matcher[ErrorOr[T]] =
    notBeAnError and (m ^^ { (e: ErrorOr[T]) => e.toOption.get })

  def beNonErrorOf[T](t: T): Matcher[ErrorOr[T]] =
    beNonErrorAnd(be_===(t))


}

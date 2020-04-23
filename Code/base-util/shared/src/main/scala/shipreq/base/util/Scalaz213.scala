package shipreq.base.util

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scalaz.Applicative
import scalaz.syntax.applicative._

object Scalaz213 {

  def traverseArraySeq[G[_], A, B](fa: ArraySeq[A])(f: A => G[B])(implicit G: Applicative[G], B: ClassTag[B]): G[ArraySeq[B]] =
    fa.foldLeft(G.pure(ArraySeq.empty[B]))((q, a) => G.apply2(q, f(a))(_ :+ _))

  def traverseArraySeq_[G[_], A, B](fa: ArraySeq[A])(f: A => G[B])(implicit G: Applicative[G]): G[Unit] =
    fa.foldLeft(G.pure(()))(_ <* f(_))

}

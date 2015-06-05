package shipreq.webapp.base.util

import monocle._
import scalaz.Applicative
import scalaz.std.stream._
import shipreq.base.util.{UnivEq, IMap}

object Optics {

  def imapTraversal[K: UnivEq, V]: Traversal[IMap[K, V], V] = {
    type I = IMap[K, V]
    val streamTraversal = PTraversal.fromTraverse[Stream, V, V]
    new PTraversal[I, I, V, V] {
      override def modifyF[F[_] : Applicative](f: V => F[V])(i: I): F[I] = {
        val c = i.clear
        val iso = Iso[I, Stream[V]](_.values.toStream)(c ++ _)
        (iso ^|->> streamTraversal).modifyF(f)(i)
      }
    }
  }

  implicit class Monocle_SetterExt[S, A, B](val x: PSetter[S, S, A, B]) extends AnyVal {
    def merge(y: PSetter[S, S, A, B]): PSetter[S, S, A, B] =
      PSetter(f => y.modify(f) compose x.modify(f))
  }

  def compositeSetters[S, A, B](h: PSetter[S, S, A, B], t: PSetter[S, S, A, B]*): PSetter[S, S, A, B] =
    t.foldLeft(h)(_ merge _)
}

package shipreq.prop.util

import scalaz.Equal
import Multimap._, Internal._

trait MultiValues[L[_]] {
  def empty[A]: L[A]
  def add1[A](a: L[A], b: A): L[A]
  def del1[A](a: L[A], b: A): L[A]
  def addn[A](a: L[A], b: L[A]): L[A]
  def deln[A](a: L[A], b: L[A]): L[A]
  def foldl[A, B](a: A, b: L[B])(f: (A, B) => A): A
  def stream[A](a: L[A]): Stream[A]
  def isEmpty[A](a: L[A]): Boolean
}

final class Multimap[K, L[_], V](val m: Map[K, L[V]])(implicit L: MultiValues[L]) {

  override def toString = m.toString()

  override def hashCode = m.hashCode

  override def equals(o: Any) = o match {
    case t: Multimap[_, _, _] => m equals t.m
    case t: Map[_, _]         => m equals t
    case _                    => false
  }

  @inline private[this] def copy(m: Map[K, L[V]]) = new Multimap[K, L, V](m)

  @inline private[this] def modM(f: Map[K, L[V]] => Map[K, L[V]]) = copy(f(m))

  def apply(k: K): L[V]            = m getOrEmpty k
  def mod  (k: K, f: L[V] => L[V]) = copy(m.mod(k, f))
  def add  (k: K, v: V)            = mod(k, _ add1 v)
  def addvs(k: K, vs: L[V])        = mod(k, _ addn vs)
  def addks(ks: L[K], v: V)        = copy(ks.foldl(m)(_.add(_, v)))
  def del  (k: K, v: V)            = mod(k, _ del1 v)
  def delk (k: K)                  = copy(m - k)
  def delv (v: V)                  = copy(m.mapValues(_ del1 v))
  def delks(ks: L[K])              = modM(m => ks.foldl(m)(_ - _))
  def delvs(vs: L[V])              = copy(m.mapValues(_ deln vs))
  def set  (k: K, vs: L[V])        = mod(k, _ => vs)
  def reverse                      = Multimap.reverse(m)

  def streamKV: Stream[(K, V)] =
    m.toStream.flatMap(kv => kv._2.stream.map(v => (kv._1, v)))

  def keyCount   = m.size
  def valueCount = m.valuesIterator.foldLeft(0)(_ + _.count)
}

object Multimap {
  implicit def multimapEqual[K, L[_], V] = Equal.equalA[Multimap[K, L, V]]

  private[util] object Internal {
    implicit final class MultiMapExt[K, L[_], V](val m: Map[K, L[V]]) extends AnyVal {
      @inline def getOrEmpty(k: K)          (implicit L: MultiValues[L]): L[V]         = m.getOrElse(k, L.empty)
      @inline def add(k: K, v: V)           (implicit L: MultiValues[L]): Map[K, L[V]] = mod(k, _ add1 v)
      @inline def mod(k: K, f: L[V] => L[V])(implicit L: MultiValues[L]): Map[K, L[V]] = put(k, f(getOrEmpty(k)))
      @inline def put(k: K, v: L[V])        (implicit L: MultiValues[L]): Map[K, L[V]] =
        if (v.isEmpty) m - k else m + (k -> v)
    }

    implicit final class MultiValueOps[L[_], A](val as: L[A]) extends AnyVal {
      def add1(b: A)                    (implicit L: MultiValues[L]) = L.add1(as, b)
      def del1(b: A)                    (implicit L: MultiValues[L]) = L.del1(as, b)
      def addn(b: L[A])                 (implicit L: MultiValues[L]) = L.addn(as, b)
      def deln(b: L[A])                 (implicit L: MultiValues[L]) = L.deln(as, b)
      def foldl[Z](z: Z)(f: (Z, A) => Z)(implicit L: MultiValues[L]) = L.foldl(z, as)(f)
      def stream                        (implicit L: MultiValues[L]) = L.stream(as)
      def set                           (implicit L: MultiValues[L]) = stream.toSet
      def count                         (implicit L: MultiValues[L]) = foldl(0)((q, _) => q + 1)
      def isEmpty                       (implicit L: MultiValues[L]) = L.isEmpty(as)
    }
  }

  def apply[K, L[_], V](m: Map[K, L[V]])(implicit L: MultiValues[L]) =
    new Multimap[K, L, V](m.filterNot(_._2.isEmpty))

  def empty[K, L[_]: MultiValues, V] =
    new Multimap[K, L, V](Map.empty)

  def reverse[A, L[_]: MultiValues, B](ab: Map[A, L[B]]): Multimap[B, L, A] =
    (empty[B, L, A] /: ab){ case (q, (a, bs)) => bs.foldl(q)(_.add(_, a)) }
}
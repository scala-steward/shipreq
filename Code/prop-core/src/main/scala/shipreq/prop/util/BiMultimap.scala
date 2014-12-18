package shipreq.prop.util

import shipreq.prop._
import shipreq.prop.util.MultiValues.Commutative

final class BiMultimap[A, L[_] : MultiValues : Commutative, B](val ab: Multimap[A, L, B],
                                                               val ba: Multimap[B, L, A]) {
  Eval.assert(prop)

  def prop =
    Eval.test("ab mirrors ba", this, ab == ba.reverse)

  override def toString = s"BiMultimap($ab, $ba)"

  override def hashCode = ab.hashCode

  override def equals(o: Any) = o match {
    case t: BiMultimap[_, _, _] => ab equals t.ab
    case _                      => false
  }

  @inline private[this] def copy(ab: Multimap[A, L, B], ba: Multimap[B, L, A]) =
    new BiMultimap[A, L, B](ab, ba)

  def add  (a: A, b: B)     = copy(ab.add(a, b)           , ba.add(b, a))

  def addas(as: L[A], b: B) = copy(ab.addks(as, b)        , ba.addvs(b, as))
  def setas(as: L[A], b: B) = copy(ab.setks(as, b)        , ba.setvs(b, as))
  def dela (a: A)           = copy(ab delk a              , ba delv a)
  def delas(as: L[A])       = copy(ab delks as            , ba delvs as)

  def addbs(a: A, bs: L[B]) = copy(ab.addvs(a, bs)        , ba.addks(bs, a))
  def setbs(a: A, bs: L[B]) = copy(ab.setvs(a, bs)        , ba.setks(bs, a))
  def delb (b: B)           = copy(ab delv b              , ba delk b)
  def delbs(bs: L[B])       = copy(ab delvs bs            , ba delks bs)

  def delab(v: A)(implicit e: A =:= B, f: B =:= A) = copy(ab delkv v, ba delkv v)
}

object BiMultimap {

  def apply[A, L[_] : MultiValues : Commutative, B](ab: Multimap[A, L, B]) =
    new BiMultimap[A, L, B](ab, ab.reverse)
}
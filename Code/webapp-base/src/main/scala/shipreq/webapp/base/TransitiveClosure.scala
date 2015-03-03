package shipreq.webapp.base

import scala.collection.immutable.BitSet
import scala.reflect.ClassTag
import scalaz.Need
import shipreq.base.util.{BiMap, UnivEq}

object TransitiveClosure {
//  def bimap[A: UnivEq](b: BiMap[A, Int])(directChildren: A => Iterable[A]) =
//    new TransitiveClosure[A](b.ab.apply, b.ba.apply, b.size, directChildren)

  def auto[A: UnivEq: ClassTag](as: TraversableOnce[A])(directChildren: A => Iterable[A]) = {
    val map = as.toStream.zipWithIndex.toMap
    val array = new Array[A](map.size)
    for ((k,v) <- map)
      array(v) = k
    new TransitiveClosure(map.apply, array.apply, array.length, directChildren)
  }
}

/**
 * Only works with acyclic digraphs.
 * Closure is also reflexive.
 *
 * Laws
 * ====
 * i ∈ [0,size)
 * a2i.i2a = id
 */
final class TransitiveClosure[A: UnivEq](a2i           : A => Int,
                                         i2a           : Int => A,
                                         size          : Int,
                                         directChildren: A => Iterable[A]) {

  private val closure: Array[Need[BitSet]] =
    new Array(size)

  // Init
  for (i <- 0 until size) {
    closure(i) = Need[BitSet] {
      val a = i2a(i)
      val z = BitSet.empty + i
      directChildren(a).foldLeft(z)((q, c) => q ++ tc(a2i(c)))
    }
  }

  @inline private def tc(i: Int): BitSet =
    closure(i).value

  def apply(a: A): Set[A] = {
    val i = a2i(a)
    tc(i).foldLeft(UnivEq.emptySet[A])(_ + i2a(_))
  }

  def nonRefl(a: A): Set[A] = {
    val i = a2i(a)
    tc(i).foldLeft(UnivEq.emptySet[A])((q, j) =>
      if (i == j) q else q + i2a(j))
  }
}

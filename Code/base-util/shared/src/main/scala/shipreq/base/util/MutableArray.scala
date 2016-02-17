package shipreq.base.util

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag

/**
  * Scala arrays don't support in-place modification.
  */
final class MutableArray[A](val underlying: Array[Any]) {
  override def toString = underlying.mkString("MutableArray[", ", ", "]")

  @inline def length = underlying.length
  @inline def isEmpty = underlying.isEmpty
  @inline def nonEmpty = underlying.nonEmpty

  def array: Array[A] =
    underlying.asInstanceOf[Array[A]]

  def map[B](f: A => B): MutableArray[B] = {
    val a = array
    var i = length
    while (i > 0) {
      i -= 1
      underlying(i) = f(a(i))
    }
    this.asInstanceOf[MutableArray[B]]
  }

  def mapOut[B, That](f: A => B)(implicit cbf: CanBuildFrom[Nothing, B, That]): That = {
    val b = cbf()
    b.sizeHint(length)
    for (a <- array)
      b += f(a)
    b.result()
  }

  @inline def sort(implicit o: Ordering[A]): MutableArray[A] = {
    scala.util.Sorting.quickSort(array)(o)
    this
  }

  def to[Col[_]](implicit cbf: CanBuildFrom[Nothing, A, Col[A]]): Col[A] =
    mapOut[A, Col[A]](identity)
}

// =====================================================================================================================

object MutableArray {

  def apply[A: ClassTag](as: TraversableOnce[A]): MutableArray[A] =
    new MutableArray(as.toArray)

  def map[A, B: ClassTag](as: Vector[A])(f: A => B): MutableArray[B] = {
    val array = new Array[Any](as.length)
    var i = 0
    as.foreach { a =>
      array(i) = f(a)
      i = i + 1
    }
    new MutableArray(array)
  }
}

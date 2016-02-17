package shipreq.base.util

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag
import MutableArray.Map

/**
  * Scala arrays don't support in-place modification.
  */
final class MutableArray[A](val array: Array[A]) {
  override def toString = array.mkString("MutableArray[", ", ", "]")

  @inline def length = array.length
  @inline def isEmpty = array.isEmpty
  @inline def nonEmpty = array.nonEmpty

  @inline def map[B](f: A => B)(implicit m: Map[A, B]): MutableArray[B] =
    m(this, f)

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
    val array = new Array[B](as.length)
    var i = 0
    as.foreach { a =>
      array(i) = f(a)
      i = i + 1
    }
    new MutableArray(array)
  }

  // ===================================================================================================================

  trait Map[A, B] {
    def apply(ma: MutableArray[A], f: A => B): MutableArray[B]
  }

  private[this] val mapAnyRefInstance = new Map[AnyRef, AnyRef] {
    override def apply(m: MutableArray[AnyRef], f: AnyRef => AnyRef): MutableArray[AnyRef] = {
      val a = m.array
      var i = a.length
      println(s"$m - $i")
      while (i > 0) {
        println(i)
        i -= 1
        a(i) = f(a(i))
      }
      m
    }
  }

  implicit def implicitMapAnyRef[A <: AnyRef, B <: AnyRef] =
    mapAnyRefInstance.asInstanceOf[Map[A, B]]
}

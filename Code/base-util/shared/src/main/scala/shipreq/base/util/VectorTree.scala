package shipreq.base.util

import scala.annotation.tailrec
import scala.collection.AbstractIterator
import ScalaExt._
import VectorTree._

final case class VectorTree[+A](children: Children[A]) extends Parent[A] {
//  override type This = VectorTree[A]

  override def getValue = None

  override def toString = "VectorTree(…)"

  def foreach[U](f: (Location, A) => U): Unit =
    locAndValueIterator(f).foreach(_ => ())

  def locAndValueIterator[B](f: (Location, A) => B): Iterator[B] =
    childrenIterator(Vector.empty, f)
}

/**
 * {root}
 *   m in [0..n): {value m}
 *     m' in [0..n'): {value m.m'}
 *       ...
 */
object VectorTree {

//  class Types[A] {
//    type Root     = VectorTree[A]
//    type Parent   = VectorTree.Parent[A]
//    type Children = VectorTree.Children[A]
//    type Node     = VectorTree.Node[A]
//    def empty: Root = VectorTree.empty
//  }

  sealed abstract class Parent[+A] {
//    type This <: Parent[A]

    val children: Children[A]
    def getValue: Option[A]

//    def setChildren[B <: (c: Children[A]): This
//
//    final def modChildren(f: Children[A] => Children[A]): This =
//      setChildren(f(children))

    final def needAtLocation(pos: Location): A =
      getAtLocation(pos) getOrElse sys.error(s"Node not found at position ${pos.whole mkString "."}.")

    final def getAtLocation(pos: Location): Option[A] = {
      val it = pos.iterator
      @tailrec
      def go(cur: Parent[A]): Option[A] = {
        val i = it.next()
        if (i >= 0 && i < cur.children.length) {
          val n = cur children i
          if (it.hasNext)
            go(n)
          else
            Some(n.value)
        } else
          None
      }
      go(this)
    }
    
    final def valueIterator: Iterator[A] =
      new AbstractIterator[A] {
        var queue = List.empty[Children[A]]
        var focus = children.iterator

        override def hasNext: Boolean =
          focus.hasNext

        override def next(): A = {
          val n = focus.next()
          if (n.children.nonEmpty)
            queue ::= n.children
          if (!focus.hasNext && queue.nonEmpty) {
            focus = queue.head.iterator
            queue = queue.tail
          }
          n.value
        }
      }

    final def childrenIterator[B](posInit: Vector[Int], f: (Location, A) => B): Iterator[B] =
      new AbstractIterator[B] {
        var index = 0
        var queue = List.empty[Iterator[B]]

        override def hasNext: Boolean =
          queue.nonEmpty || index < children.length

        override def next(): B =
          queue match {
            case Nil =>
              val n = children(index)
              val p = NonEmptyVector.end(posInit, index)
              val b = f(p, n.value)

              index += 1
              val i = n.childrenIterator(p.whole, f)
              if (i.hasNext)
                queue ::= i

              b

            case qh :: qt =>
              val b = qh.next()
              if (!qh.hasNext)
                queue = qt
              b
          }
      }
  }

  type Children[+A] = Vector[Node[A]]

  final case class Node[+A](value: A, children: Children[A]) extends Parent[A] {
//    override type This = Node[A]
    override def getValue = Some(value)

//    def valuesWithLocations[B](p: Location, f: (Location, A) => B): Iterator[B] =
//      Iterator.single(f(p, value)) ++ iterateChildren(p.whole, f)
  }

  type Location = NonEmptyVector[Int]

  @inline def noChildren: Children[Nothing] =
    Vector.empty

  val empty: VectorTree[Nothing] =
    VectorTree(noChildren)

  implicit def univEqForNode[A: UnivEq]: UnivEq[Node[A]]       = UnivEq.derive
  implicit def univEqForRoot[A: UnivEq]: UnivEq[VectorTree[A]] = UnivEq.derive
}

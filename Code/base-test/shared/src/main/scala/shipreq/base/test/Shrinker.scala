package shipreq.base.test

import japgolly.microlibs.stdlib_ext.StdlibExt._
import scala.collection.View

final case class Shrinker[A](shrink: A => View[RoseTree[A]]) extends AnyVal {

  def start(a: A): RoseTree[A] =
    RoseTree(a, shrink(a))
}

object Shrinker {

  def iterator[A](f: A => Iterator[RoseTree[A]]): Shrinker[A] =
    Shrinker(a => View.fromIteratorProvider(() => f(a)))

  def combine[A](ss: Shrinker[A]*): Shrinker[A] = {
    val fs = ss.toVector
    var self: Shrinker[A] = apply(null)
    self =
      fs.length match {
        case 0 => Shrinker(_ => View.empty)
        case 1 => fs.head
        case _ => iterator {
          root =>
            fs.iterator.flatMap(_.shrink(root)).map { childTree =>
              // Discard children (by f) and recreate (by fs)
              val child = childTree.value
              RoseTree(child, self.shrink(child))
            }
        }
      }
    self
  }

  def vectorRemoveOne[A]: Shrinker[Vector[A]] = {
    var self: Shrinker[Vector[A]] = apply(null)
    self = iterator { root =>
      root.indices.iterator.map { idx =>
        val child = root.deleteOrNull(idx)
        RoseTree(child, self.shrink(child))
      }
    }
    self
  }

  def vectorShrinkElement[A](f: Shrinker[A]): Shrinker[Vector[A]] = {
    var self: Shrinker[Vector[A]] = apply(null)
    self = iterator { root =>
      root.indices.iterator.flatMap { idx =>
        f.shrink(root(idx)).map { newElement =>
          val child = root.updated(idx, newElement.value)
          RoseTree(child, self.shrink(child))
        }
      }
    }
    self
  }

  def maybe[A](accept: A => Boolean, f: A => A): Shrinker[A] = {
    var self: Shrinker[A] = apply(null)
    self = iterator { root =>
      if (accept(root)) {
        val a = f(root)
        Iterator.single(RoseTree(a, self.shrink(a)))
      } else
        Iterator.empty
    }
    self
  }


}
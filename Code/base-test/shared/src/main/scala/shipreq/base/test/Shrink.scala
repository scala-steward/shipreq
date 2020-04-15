package shipreq.base.test

import shipreq.base.util._

object Shrink {

  val DefaultBreadthLimit = 3

  def apply[A](a           : A)
              (shrinker    : Shrinker[A],
               size        : A => Int,
               validity    : A => Validity,
               breadthLimit: Int = DefaultBreadthLimit): A = {

    def go(root: RoseTree[A]): A = {
      val rootSize = size(root.value)
      val candidates =
        root.children.iterator.flatMap { child =>
          val childSize = size(child.value)
          if (childSize < rootSize && validity(child.value).is(Invalid))
            (go(child), childSize) :: Nil
          else
            Nil
        }.take(breadthLimit)
      chooseSmallest(root.value, rootSize, candidates)
    }

    go(shrinker.start(a))
  }

  private[test] def chooseSmallest[A](root: A, rootSize: Int, candidates: IterableOnce[(A, Int)]): A = {
    var best = (root, rootSize)
    for (c <- candidates.iterator) {
      if (c._2 < best._2)
        best = c
    }
    best._1
  }
}

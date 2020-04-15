package shipreq.base.test

import shipreq.base.util._
import shipreq.base.util.FxModule._

object ShrinkFx {
  import Shrink.{DefaultBreadthLimit, chooseSmallest}

  def apply[A](a           : A)
              (shrinker    : Shrinker[A],
               size        : A => Int,
               validity    : A => Fx[Validity],
               breadthLimit: Int = DefaultBreadthLimit): Fx[A] = {

    def go(root: RoseTree[A]): Fx[A] = {
      val rootSize = size(root.value)

      val candidates: Fx[Vector[(RoseTree[A], Int)]] =
        root
          .children
          .iterator
          .flatMap { child =>
            val childSize = size(child.value)
            if (childSize < rootSize)
              validity(child.value).map((child, childSize, _)) :: Nil
            else
              Nil
          }
          .foldLeft(Fx.pure(Vector.empty[(RoseTree[A], Int)])) {case (ioRes, ioNext) =>
            ioRes.flatMap { res =>
              if (res.length >= breadthLimit)
                Fx.pure(res)
              else
                ioNext.map {
                  case (_, _, Valid)   => res
                  case (c, s, Invalid) => res.appended((c, s))
                }
            }
          }

      candidates.map(cs => chooseSmallest(root, rootSize, cs))
        .flatMap(child => if (child ne root) go(child) else Fx.pure(root.value))
    }

    go(shrinker.start(a))
  }
}

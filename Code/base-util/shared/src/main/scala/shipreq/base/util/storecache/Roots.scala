package shipreq.base.util.storecache

import shipreq.base.util.{FreeOption, LazyVal}

final case class Next[A](value: A, changed: LazyVal[Boolean])

final class StoreCache1[I, S, A](mapInput   : I => S,
                                 source     : LazyVal[S],
                                 val lazyVal: LazyVal[A])
                                (val root   : FreeOption[StoreCache1[I, S, A]])
                                (implicit qe: QuickEq[S]) extends StoreCache[I, A] {

  override def value: A =
    lazyVal.value

  override def contramap[J](f: J => I): StoreCache1[J, S, A] =
    new StoreCache1(mapInput compose f, source, lazyVal)(root.map(_.contramap(f)))

  override def map[B](f: A => B): StoreCache1[I, S, B] =
    new StoreCache1(mapInput, source, lazyVal map f)(root.map(_.map(f)))

  private[storecache] def nextStrict(nextInput: I, run: S => A): StoreCache1[I, S, A] = {
    val nextSource = mapInput(nextInput)
    if (qe.areEq(source.value, nextSource))
      this
    else
      new StoreCache1(mapInput, LazyVal.pure(nextSource), LazyVal(run(nextSource)))(FreeOption.empty)
  }

  private[storecache] def nextLazy(nextInput: => I, run: S => A): Next[StoreCache1[I, S, A]] =
    if (lazyVal.isEvaluated()) {

      // Previous has been evaluated
      // Mark it as a "root" and return a lazy next step
      val s1 = source.value
      val newSource: LazyVal[Either[S, S]] = LazyVal {
        val s2 = mapInput(nextInput)
        if (qe.areEq(s1, s2))
          Left(s1)
        else
          Right(s2)
      }
      val newValue: LazyVal[A] =
        newSource.flatMap {
          case Left(_) => lazyVal
          case Right(s) => LazyVal(run(s))
        }
      val newRoot = this.withoutRoot // Disallow chains of roots. The whole point is to avoid chains.
      val sc = new StoreCache1(mapInput, newSource.map(_.merge), newValue)(FreeOption(newRoot))
      Next(sc, newSource.map(_.isRight))

    } else root.getOrNull match {

      case null =>
        // This chain has never been evaluated. There's nothing to reuse.
        // Forget it and return a new lazy result
        val ls = LazyVal(mapInput(nextInput))
        val la = ls.map(run)
        val sc = new StoreCache1(mapInput, ls, la)(FreeOption.empty)
        Next(sc, LazyVal.True)

      case r =>
        // Previous step not evaluated but there is an evaluated result in its history.
        // See if its reusable.
        r.nextLazy(nextInput, run)
    }

  private def withoutRoot: StoreCache1[I, S, A] =
    if (root.isEmpty)
      this
    else
      new StoreCache1(mapInput, source, lazyVal)(FreeOption.empty)
}

final class Logic1[I, S: QuickEq, A](mapInput: I => S, run: S => A) extends StoreCache.Logic[I, A] {
  override type Cache = StoreCache1[I, S, A]

  override def contramap[J](f: J => I): Logic1[J, S, A] =
    new Logic1(mapInput compose f, run)

  override def map[B](f: A => B): Logic1[I, S, B] =
    new Logic1(mapInput, f compose run)

  override def initStrict(init: I): Cache = {
    val s = mapInput(init)
    val ls = LazyVal.pure(s)
    val la = ls.map(run)
    new StoreCache1(mapInput, ls, la)(FreeOption.empty)
  }

  override def nextStrict(prev: Cache, i: I): Cache =
    prev.nextStrict(i, run)

  override def initLazy(init: => I): Cache = {
    val ls = LazyVal(mapInput(init))
    val la = ls.map(run)
    new StoreCache1(mapInput, ls, la)(FreeOption.empty)
  }

  override def nextLazyFull(prev: Cache, i: => I): Next[Cache] =
    prev.nextLazy(i, run)
}


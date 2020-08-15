package shipreq.base.test

import nyaya.gen.{Gen, GenSize}

final class CachedGen[+A](val gen: Gen[A],
                          val values: LazyList[A]) {

  def cachedGen(): Gen[A] = {
    val it = values.iterator
    Gen.point(it.next())
  }

  def map[B](f: A => B): CachedGen[B] =
    new CachedGen(gen map f, values map f)

  def flatMap[B](f: Gen[A] => CachedGen[B]): CachedGen[B] = {
    val rnd = f(gen)
    val ll = f(cachedGen())
    new CachedGen(rnd.gen, ll.values)
  }

  def flatMapGen[B](f: Gen[A] => Gen[B]): CachedGen[B] =
    flatMap(g => CachedGen(f(g)))

  def flatMapGen[B](genSize: Int, f: Gen[A] => Gen[B]): CachedGen[B] =
    flatMap(g => CachedGen.withGenSize(genSize, f(g)))
}

object CachedGen {

  def apply[A](gen: Gen[A]): CachedGen[A] =
    apply(gen, GenSize.Default)

  def apply[A](gen: Gen[A], genSize: GenSize): CachedGen[A] =
    new CachedGen(gen, LazyList.from(gen.samples(genSize)))

  def withGenSize[A](genSize: Int, gen: Gen[A]): CachedGen[A] =
    apply(gen, GenSize(genSize))

}
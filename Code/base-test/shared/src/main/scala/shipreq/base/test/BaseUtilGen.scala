package shipreq.base.test

import nyaya.gen._
import shipreq.base.util._
import SizeSpec.DisableDefault._

object BaseUtilGen {

  implicit def BaseUtilGen_GenExt[A](g: Gen[A]) = new BaseUtilGen_GenExt(g.run)
  class BaseUtilGen_GenExt[A](private val _g: Gen.Run[A]) extends AnyVal {
    private implicit def g = Gen(_g)

    def nev(implicit ss: SizeSpec): Gen[NonEmptyVector[A]] =
      for {t <- g.vector(ss); h <- g} yield NonEmptyVector(h, t)

    def nes(implicit ss: SizeSpec, ev: UnivEq[A]): Gen[NonEmptySet[A]] =
      for {t <- g.set(ss); h <- g} yield NonEmptySet(h, t)
  }

  def genISubset[A: UnivEq](g: Gen[NonEmptySet[A]]): Gen[ISubset[A]] =
    Gen.chooseGen(
      Gen pure ISubset.All(),
      g map ISubset.Only.apply,
      g map ISubset.Not.apply)

  def genMTrie[K: UnivEq, V](genK: Gen[K], genV: Gen[V], maxDepth: Int)(implicit ss: SizeSpec): Gen[MTrie.Trie[K, V]] = {
    val valueN   = genV map MTrie.Value[K, V]
    val valueO   = valueN.option
    val midDepth = maxDepth >> 1

    def level(depth: Int): Gen[MTrie.Trie[K, V]] =
      if (depth <= 1)
        Gen pure Map.empty
      else {
        val branch  = Gen.apply2(MTrie.Branch[K, V])(valueO, level(depth - 1))
        val branchN = branch.flatMap[MTrie.Node[K, V]](b => if (b.next.nonEmpty) Gen.pure(b) else b.value.fold(valueN)(Gen.pure))
        val node    = Gen.chooseGen(branchN, valueN, if (depth > midDepth) branchN else valueN)
        node.mapBy(genK)(ss)
      }

    level(maxDepth)
  }

  def genNonEmptySetDiff[A: UnivEq](g: Gen[A])(implicit ss: SizeSpec): Gen[NonEmpty[SetDiff[A]]] = {
    val set = g.set(ss)
    val attempt =
      for {
        a <- set
        b <- set
      } yield SetDiff(a, b &~ a)
    attempt.flatMap(d =>
      NonEmpty(d) match {
        case Some(ne) => Gen pure ne
        case None     => g.map(a => NonEmpty.force(SetDiff(Set.empty[A], Set(a))))
      }
    )
  }

}

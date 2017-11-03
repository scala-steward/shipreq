package shipreq.webapp.base.hash2

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.microlibs.stdlib_ext.StdlibExt._
import japgolly.univeq._
import shipreq.base.util.EqualsByRef

//trait EvoHashModule {
//
//  type Scope
//  type Data
//  protected def univEqScope: UnivEq[Scope]
//  protected def univEqData: UnivEq[Data]

abstract class EvoHashModule[_Scope: UnivEq, _Data] {
  final type Scope = _Scope
  final type Data = _Data

  protected val schemeRegistry: Schemes

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final type ScopeVer = EvoHashModule.ScopeVer
  final val  ScopeVer = EvoHashModule.ScopeVer

  final type SchemeId = EvoHashModule.SchemeId
  final val  SchemeId = EvoHashModule.SchemeId

  final type VersionedHashFn = EvoHashModule.VersionedHashFn[Data]
  final val  VersionedHashFn = EvoHashModule.VersionedHashFn

  final type ScopesTo[+A] = Map[Scope, A]

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final class Schemes(schemesWithoutIds: NonEmptyVector[SchemeId => Scheme]) {

    val schemes: NonEmptyVector[Scheme] =
      schemesWithoutIds.mapWithIndex((f, i) => f(SchemeId(i)))

    val latest: Scheme =
      schemes.last

    val latestId: SchemeId =
      latest.id

    private[this] val allWhole = schemes.whole

    def unsafeGet(id: SchemeId): Scheme =
      allWhole(id.index)

    import Schemes.EvolutionOp

    def addEvolution(op1: EvolutionOp, opN: EvolutionOp*): Schemes = {
      val newScopes: ScopesTo[VersionedHashFn] =
        (op1 +: opN).foldLeft(latest.hashFns) { (cur, op) =>

          def assertScopeExists(s: Scope) = {
            assert(cur.contains(s), s"Evolution error! Scheme doesn't contain scope: $s")
            cur(s)
          }

          def assertScopeDoesntExist(s: Scope): Unit =
            assert(!cur.contains(s), s"Evolution error! Scheme already contains scope: $s")

          op match {
            case EvolutionOp.Add((s, h)) =>
              assertScopeDoesntExist(s)
              cur.updated(s, VersionedHashFn.init(h))

            case EvolutionOp.Evolve((s, h)) =>
              val old = assertScopeExists(s)
              cur.updated(s, old.addEvolution(h))

            case EvolutionOp.Drop(s) =>
              assertScopeExists(s)
              cur - s
          }
        }

      new Schemes(schemesWithoutIds :+ Scheme.withoutId(newScopes))
    }
  }

  object Schemes {

    sealed trait EvolutionOp
    object EvolutionOp {
      case class Add   (kv: (Scope, HashFn[Data])) extends EvolutionOp
      case class Evolve(kv: (Scope, HashFn[Data])) extends EvolutionOp
      case class Drop  (k: Scope)                  extends EvolutionOp
    }

    def init(value1: (Scope, HashFn[Data]), values: (Scope, HashFn[Data])*): Schemes =
      one(Scheme.withoutId((value1 +: values).toMap.mapValuesNow(VersionedHashFn.init)))

    def one(f: SchemeId => Scheme): Schemes =
      new Schemes(NonEmptyVector one f)
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final case class Scheme(id: SchemeId, hashFns: ScopesTo[VersionedHashFn]) extends EqualsByRef {

    override def toString = s"HashScope(${id.asChar})"

    def hash(data: Data): ScopesTo[Int] =
      hashFns.mapValuesNow(_.hashFn(data))
  }

  object Scheme {
    def withoutId(hashFns: ScopesTo[VersionedHashFn]): SchemeId => Scheme =
      apply(_, hashFns)
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final type HashRecs = Map[Scheme, ScopesTo[Option[Int]]]

  object HashRecs {

    val empty: HashRecs =
      UnivEq.emptyMap

    private val latestScheme = schemeRegistry.latest

    def full(p: Data): HashRecs =
      empty.updated(latestScheme, latestScheme.hash(p).mapValuesNow(Some(_)))

    def changes(p1: Data, p2: Data): HashRecs =
      __changes(latestScheme, p1, p2)

    /** Public for testing */
    def __changes(scheme: Scheme, p1: Data, p2: Data): HashRecs = {
      var r = Map.empty[Scope, Option[Int]]
      for (kv <- scheme.hashFns) {
        val scope = kv._1
        val hashFn = kv._2.hashFn
        val h1 = hashFn(p1)
        val h2 = hashFn(p2)
        if (h1 !=* h2)
          r += scope -> Some(h2)
      }
      empty.updated(scheme, r)
    }
  }

}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object EvoHashModule {

  final case class SchemeId(index: Int) extends AnyVal {
    def asChar: Char =
      ('a' + index).toChar
  }

  object SchemeId {
    implicit def univEq: UnivEq[SchemeId] =
      UnivEq.derive

    def fromChar(char: Char): SchemeId =
      apply(char - 'a')
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final case class ScopeVer(value: Int) extends AnyVal {
    @inline def inc: ScopeVer =
      ScopeVer(value + 1)

    @inline def <=(x: ScopeVer): Boolean =
      value <= x.value
  }

  object ScopeVer {
    val init: ScopeVer =
      apply(1)
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final case class VersionedHashFn[A](ver: ScopeVer, hashFn: HashFn[A]) {
    def addEvolution(hashFn: HashFn[A]): VersionedHashFn[A] =
      VersionedHashFn(ver.inc, hashFn)
  }

  object VersionedHashFn {
    def init[A](hashFn: HashFn[A]): VersionedHashFn[A] =
      apply(ScopeVer.init, hashFn)
  }

}
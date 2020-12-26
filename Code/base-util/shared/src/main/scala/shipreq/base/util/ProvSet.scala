package shipreq.base.util

import ProvSet._
import PartialOrder.ImplicitOps._

/** Provenance Set.
 *
 * This is a state-based CRDT/CvRDT (conflict-free, convergent, replicated data type).
 *
 * Laws:
 *   - idempotency
 *   - associativity
 *   - commutativity
 *
 * This is modelled in `../TLA+/provset.tla`.
 */
final case class ProvSet[K, V, M](repr: Repr[K, V, M])(implicit module: Module[K, V, M]) {
  import module.{one, partialOrder}

  type Self = ProvSet[K, V, M]
  type Entry = ProvSet.Entry[K, Value[V], M]

  @inline def isEmpty  = repr.isEmpty
  @inline def nonEmpty = repr.nonEmpty

  def ++(s: Self): Self =
    s.repr.foldLeft(this)(_ + _)

  def +(add: Entry): Self = {

    def addProv(p: Set[K], k: K): Set[K] =
      p.find(_ isComparableTo k) match {
        case None    => p + k
        case Some(j) =>
          if (j >= k)
            p
          else
            p - j + k
      }

    def mergeProvs(x: Set[K], y: Set[K]): Set[K] =
      y.foldLeft(x)(addProv)

    def mergeEntries(e: Entry, into: Entry): Entry = {
      val newProv =
        addProv(e.provenance, e.key).filterNot { k =>
          // No need for this check. As verified in ../TLA+/provset.tla this will never occur with monotonic clocks &
          // gossiping (as is the case with Drafts).
          // assert(!(k > into.key), s"Discarding provenance $k in order to add ${e.key} to ${into.key}")
          k <= into.key
        }

      val metadataPair =
        MergePair(
          subject        = e.metadata,
          into           = into.metadata,
          subjectGreater = e.key > into.key)

      Entry(
        key        = into.key,
        value      = into.value,
        metadata   = module.mergeMetadata(metadataPair),
        provenance = mergeProvs(into.provenance, newProv)
      )
    }

    @tailrec
    def go(base: Set[Entry], e: Entry): Set[Entry] =
      base.find(into => e.key.isComparableTo(into.key) || into.provenance.exists(e.key <= _)) match {
        case None    => base + e
        case Some(i) =>
          val merged =
            if (e.key > i.key)
              mergeEntries(i, into = e)
            else
              mergeEntries(e, into = i)
          go(base - i, merged)
      }

    if (isEmpty)
      one(add)
    else if (repr contains add)
      this
    else
      ProvSet(go(repr, add))
  }
}

object ProvSet {

  type Repr[K, V, M] = Set[Entry[K, Value[V], M]]

  final case class Entry[K, V, M](key       : K,
                                  value     : V,
                                  metadata  : M,
                                  provenance: Set[K])

  sealed trait Value[+A] {
    def toOption: Option[A]
  }

  object Value {
    final case class Live[+A](value: A) extends Value[A] {
      override def toOption = Some(value)
    }

    case object Tombstone extends Value[Nothing] {
      override def toOption = None
    }
  }

  final case class MergePair[+A](subject: A, into: A, subjectGreater: Boolean) {
    def greater: A = if (subjectGreater) subject else into
    def lesser : A = if (subjectGreater) into else subject
  }

  implicit def univEqV[V: UnivEq                      ]: UnivEq[Value[V]        ] = UnivEq.derive
  implicit def univEqE[K: UnivEq, V: UnivEq, M: UnivEq]: UnivEq[Entry[K, V, M]  ] = UnivEq.derive
  implicit def univEq [K: UnivEq, V: UnivEq, M: UnivEq]: UnivEq[ProvSet[K, V, M]] = UnivEq.derive

  object Module {
    def apply[K: PartialOrder : UnivEq, V: UnivEq, M: UnivEq](mergeMetadata: MergePair[M] => M): Module[K, V, M] =
      new Module[K, V, M](mergeMetadata)
  }

  final class Module[K, V, M](val mergeMetadata: MergePair[M] => M)
                             (implicit
                              val partialOrder: PartialOrder[K],
                              val univEqK: UnivEq[K],
                              val univEqV: UnivEq[V],
                              val univEqM: UnivEq[M],
                             ) {

    type ProvSet = shipreq.base.util.ProvSet[K, V, M]
    type Value   = shipreq.base.util.ProvSet.Value[V]
    type Entry   = ProvSet.Entry[K, Value, M]

    implicit def univEq: UnivEq[ProvSet] =
      ProvSet.univEq

    val empty: ProvSet =
      ProvSet[K, V, M](Set.empty)(this)

    val entry = Entry.apply[K, Value, M] _

    def one(entry: Entry): ProvSet =
      ProvSet[K, V, M](Set.empty[Entry] + entry)(this)

    def consolidate(entries: Entry*): ProvSet =
      entries.foldLeft(empty)(_ + _)
  }

  // ===================================================================================================================

  final class Laws[K, V, M](module: Module[K, V, M]) {
    import nyaya.prop._
    import scalaz.Equal

    private implicit val equality = scalazEqualFromUnivEq(module.univEq)

    type ProvSet = shipreq.base.util.ProvSet[K, V, M]
    type Entry   = shipreq.base.util.ProvSet.Entry[K, Value[V], M]
    type Input   = (ProvSet, ProvSet, ProvSet)
    type Laws    = Prop[Input]

    private def equal2[B: Equal](name: String,
                                 e: (ProvSet, ProvSet) => B,
                                 a: (ProvSet, ProvSet) => B): Laws = {
      def f(desc: String, f1: Input => ProvSet, f2: Input => ProvSet) = {
        def mk(g: (ProvSet, ProvSet) => B): Input => B = i => g(f1(i), f2(i))
        Prop.equal[Input, B](s"$name ($desc)", mk(a), expect = mk(e))
      }
      f("1,2", _._1, _._2) & f("1,3", _._1, _._3) & f("2,3", _._2, _._3)
    }

    private val idempotency: Laws =
      equal2("idempotency",
        (x, y) => (x ++ y) ++ y,
        (x, y) => x ++ y)

    private val associativity: Laws =
      Prop.equal[Input, ProvSet]("associativity",
        i => (i._1 ++ i._2) ++ i._3,
        i => i._1 ++ (i._2 ++ i._3))

    private val commutativity: Laws =
      equal2("commutativity",
        (x, y) => x ++ y,
        (x, y) => y ++ x)

    // TODO: Assert invariants encoded in TLA+ spec
    // Assert self never in own prov
    // Assert there should be no two items in prov that are comparable

    val laws: Laws =
      idempotency & associativity & commutativity
  }
}

package shipreq.base.util

import ProvSet._
import PartialOrder.ImplicitOps._
import shipreq.base.util.PartialOrder.Cmp
import shipreq.base.util.PartialOrder.Cmp._

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
  import module.{one, partialOrderE, partialOrderK}

  type Self = ProvSet[K, V, M]
  type Entry = ProvSet.Entry[K, Value[V], M]

  @inline def isEmpty  = repr.isEmpty
  @inline def nonEmpty = repr.nonEmpty

  @elidable(elidable.FINEST)
  override def toString =
    repr.iterator.map(_.toString).toList.sorted.mkString("ProvSet(", ",\n        ", ")")

  @elidable(elidable.ASSERTION)
  def assertProps_(msg: => String = ""): Unit =
    (new Props(module))
      .provSet(this)
      .rename(_ => scalaz.Value("ProvSet.assertProps()" + Option(msg).filter(_.nonEmpty).fold("")(" - " + _)))
      .assertSuccess()

  @inline
  def assertProps(msg: => String = ""): this.type = {
    assertProps_(msg)
    this
  }

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

//    def mergeEntries(e1: Entry, e2: Entry): Entry =
//      if (e1.key >= e2.key)
//        mergeEntriesL(e2, e1)
//      else
//        mergeEntriesL(e1, e2)

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

//    def canMergeInto(e: Entry, into: Entry): Boolean =
//      e.key >= into.key || into.provenance.exists(e.key <= _)

    // TODO: Update TLA+
    @tailrec
    def go(base: Set[Entry], e: Entry): Set[Entry] = {

      val omg = (s: String) => s == "ProvSet({B1 = Tombstone (MB1) < {A0,C2}})" ||
                               s == "ProvSet({C0 = Live(C=0) (MC0) < {B2}})"
      val omge = (e: Entry) => omg(e.toString)
//      val debug = omge(e) && base.exists(omge)
      val debug = !true

//      val mos = base.filter(f =>
//        e.key.isComparableTo(f.key)
//          || f.provenance.exists(e.key isComparableTo _)
//          || e.provenance.exists(f.key isComparableTo _)
//      )
//
//      println(s"(${mos.size}) $mos")
//
//      mos.headOption match {
//        case None    => base + e
//        case Some(i) =>
//          val merged = {
////            if (i.key isComparableTo e.key)
//              mergeEntries(e, i)
////            else
//          }
//          go(base - i, merged)
//      }

//      var mo1 = base.find(into => e.key.isComparableTo(into.key) || into.provenance.exists(e.key <= _))
      var mo1 = base.find(_ isComparableTo e)
      var mo2 = Option.empty[Entry] // base.find(f => e.provenance.exists(f.key <= _))

//      if (mo1.isDefined && mo2.isDefined) {
      if (debug) {
        println(
          s"""=======================================================================================
             |$base + $e
             |
             |mo1: $mo1
             |mo2: $mo2
             |
             |""".stripMargin)
//        val x = mo1.get
//        val y = mo2.get
//        if (x.key >= y.key)
//          mo1 = None
//        else
//        mo2 = None
//        println(
//          s"""mo1: $mo1
//             |mo2: $mo2
//             |
//             |""".stripMargin)
      }

//      (mo1, mo2) match {
//        case (None, None) =>
//          base + e
//
//        case (Some(i), None) =>
//          go(base - i, mergeEntries(e, i))
//
//        case (None, Some(i)) =>
//          go(base - i, mergeEntries(e, i))
//
//        case (Some(x), Some(y)) =>
//          ???
//          println(
//            s"""===============================================================================
//               |
//               |base = $base
//               |e    = $e
//               |x    = $x
//               |y    = $y
//               |x>y  = ${x.key > y.key}
//               |""".stripMargin)
//
//          val i = x
//          go(base - i, mergeEntries(e, i))
//      }

      mo1 match {
        case None =>
          mo2 match {
            case None =>
              base + e
            case Some(m) =>

              val merged = mergeEntries(m, into = e)
              go(base - m, merged)
          }

        case Some(i) =>
          val merged =
            if (e < i)
              mergeEntries(e, into = i)
            else
              mergeEntries(i, into = e)

//            partialOrderK(e.key, i.key) match {
//              case Greater  => mergeEntries(i, into = e)
//              case Equal
//                 | Lesser   => mergeEntries(e, into = i)
//              case Separate =>
////                val x = i.provenance.forall(_ < e.key)
//                val x = i.provenance.exists(e.key > _)
//                val y = e.provenance.exists(i.key > _)
//                if (debug) println(s"[1] e = $e")
//                if (debug) println(s"[1] i = $i")
//                if (debug) println(s"[1] $x / $y / ${module.isAscendingE(e, i)} / ${module.partialOrderE(e, i)} / ${module.partialOrderE(i, e)}")
//
//                (x, y) match {
//                  case (true, false) => mergeEntries(i, into = e)
//                  case (false, true) => mergeEntries(e, into = i)
//                  case _ =>
//                    if (module.isAscendingE(e, i))
//                      mergeEntries(i, into = e)
//                    else
//                      mergeEntries(e, into = i)
//                }
//            }

          go(base - i, merged)
      }
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
                                  provenance: Set[K]) {
    @elidable(elidable.FINEST)
    override def toString = {
      val prov = if (provenance.isEmpty) "" else provenance.iterator.map(_.toString).toList.sorted.mkString(" ≤{", ",", "}")
      s"{$key = $value ($metadata)$prov}"
    }
  }

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
    def apply[K: PartialOrder : UnivEq, V: UnivEq, M: UnivEq](mergeMetadata: MergePair[M] => M,
                                                              isAscending: (K, K, M, M) => Boolean): Module[K, V, M] =
      new Module[K, V, M](mergeMetadata, isAscending)
  }

  final class Module[K, V, M](val mergeMetadata: MergePair[M] => M,
                              val isAscending: (K, K, M, M) => Boolean)
                             (implicit
                              val partialOrderK: PartialOrder[K],
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
      entries.foldLeft(empty)((s, e) => (s + e).assertProps(s"$s + $e"))

    val isAscendingE: (Entry, Entry) => Boolean =
      (x, y) => isAscending(x.key, y.key, x.metadata, y.metadata)

    implicit val partialOrderE: PartialOrder[Entry] =
      PartialOrder((x, y) =>
        partialOrderK(x.key, y.key) match {
          case Separate =>
            val `x<=y` = y.provenance.exists(x.key <= _)
            val `y<=x` = x.provenance.exists(y.key <= _)
            (`x<=y`, `y<=x`) match {
              case (false, false) => Separate
              case (true , false) => Lesser
              case (false, true ) => Greater
              case (true , true ) => if (isAscendingE(x, y)) Lesser else Greater
            }

          case byKey =>
            byKey
        }
      )
  }

  // ===================================================================================================================

  final class Props[K, V, M](val module: Module[K, V, M]) {
    import nyaya.prop._
    import module.{Entry => E, ProvSet => S, partialOrderK}
    import ScalazExtra._

    type Prov = Set[K]

    def prov: Prop[Prov] =
      Prop.atom[Prov]("Provenance", ps => {
        var failure = Option.empty[String]
        for {
          p <- ps
          q <- ps - p
        } if (p isComparableTo q)
            failure = Some("Comparable keys in same provenance.")
        failure
      })

    def entry: Prop[E] = {
      def keyAndProv =
        Prop.forall[E, Set, K](_.provenance)(e =>
          Prop.atom("Entry key & provenance", p => Option.when(p <= e.key)(
            s"Entry ${e.key} has in its provenance, a key $p that is <= itself.")))

      (keyAndProv & prov.contramap((_: E).provenance)).rename("entry")
    }

    def provSet: Prop[S] =
      Prop.forall[S, Set, E](_.repr) { s =>

        def compareEntries(name: => String)(c: (E, E) => Boolean): Prop[E] =
          Prop.test[E](name, e => s.repr.forall(f => (e eq f) || c(e, f)))

        def coexistenceK: Prop[E] =
          compareEntries("Comparable sibling entries.")((e, f) =>
            e.key.isSeparateTo(f.key))

        def coexistenceP: Prop[E] =
          compareEntries("Sibling provenance.")((e, f) =>
            f.provenance.forall(p => !(e.key <= p)))

        (entry & coexistenceK & coexistenceP).rename("provSet")
      }
  }

  // ===================================================================================================================

  object Laws {
    final case class Input[K, V, M](a: ProvSet[K, V, M],
                                    b: ProvSet[K, V, M],
                                    c: ProvSet[K, V, M]) {
      import japgolly.microlibs.stdlib_ext.StdlibExt._

      override def toString =
        s"""ProvSet.Laws.Input(
           |  a = ${a.toString.indent(6).trim},
           |  b = ${b.toString.indent(6).trim},
           |  c = ${c.toString.indent(6).trim})
           |""".stripMargin.trim
    }
  }

  final class Laws[K, V, M](module: Module[K, V, M]) {
    import nyaya.prop._
    import scalaz.Equal

    private implicit val equality = scalazEqualFromUnivEq(module.univEq)

    type ProvSet = shipreq.base.util.ProvSet[K, V, M]
    type Entry   = shipreq.base.util.ProvSet.Entry[K, Value[V], M]
    type Input   = Laws.Input[K, V, M]
    type Laws    = Prop[Input]

    private def prop2[A](name: String, p: Prop[A])(g: (ProvSet, ProvSet) => A): Laws = {
      def f(desc: String, f1: Input => ProvSet, f2: Input => ProvSet): Laws =
        Prop.evaln(s"$name ($desc)", i => {
          val a = g(f1(i), f2(i))
          p(a).liftL
        })
      f("a,b", _.a, _.b) & f("a,c", _.a, _.c) & f("b,c", _.b, _.c)
    }

    private def equal2[B: Equal](name: String,
                                 e: (ProvSet, ProvSet) => B,
                                 a: (ProvSet, ProvSet) => B): Laws = {
      def f(desc: String, f1: Input => ProvSet, f2: Input => ProvSet): Laws = {
        def mk(g: (ProvSet, ProvSet) => B): Input => B = i => g(f1(i), f2(i))
        Prop.equal[Input, B](s"$name ($desc)", mk(a), expect = mk(e))
      }
      f("a,b", _.a, _.b) & f("a,c", _.a, _.c) & f("b,c", _.b, _.c)
    }

    private val idempotency: Laws =
      equal2("idempotency",
        (x, y) => (x ++ y) ++ y,
        (x, y) => x ++ y)

    private val associativity: Laws =
      Prop.equal[Input, ProvSet]("associativity",
        i => (i.a ++ i.b) ++ i.c,
        i => i.a ++ (i.b ++ i.c))

    private val commutativity: Laws =
      equal2("commutativity",
        (x, y) => x ++ y,
        (x, y) => y ++ x)

    private val validity: Laws = {
      val props = new Props(module)
      prop2("validity", props.provSet)(_ ++ _)
    }

    val laws: Laws =
      List(
        idempotency,
        associativity,
        commutativity,
        validity,
      ).reduce(_ & _)
  }
}

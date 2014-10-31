package shipreq.prop

import scala.annotation.elidable
import scalaz.{Foldable, NonEmptyList}
import scalaz.std.tuple.tuple2Monoid
import scalaz.std.list.listMonoid
import scalaz.std.set.setMonoid
import scalaz.syntax.foldable._
import shipreq.prop.util.{Monoidmap, Multimap}

object Prop {

  @inline final def apply[A](name: String, t: A => Boolean): Prop[A] =
    atom(name, t, identity)

  @inline final def atom[A](name: String, t: A => Boolean, fmta: A => Any): Prop[A] =
    new Atom[A](name, t, fmta)

  def distinct[A, B](name: String, f: A => Stream[B]) =
    distinct[B](name).contramap(f)

  def distinct[A](name: String) =
    atom[Stream[A]](s"each $name is unique", as => {
      var s = Set.empty[A]
      as.forall(a =>
        s.contains(a) match {
          case true  => false
          case false => s += a; true
        })
    }, as => {
      val dups =
        (Map.empty[A, Int] /: as)((q, a) => q + (a -> (q.getOrElse(a, 0) + 1)))
          .filter(_._2 > 1)
          .toStream
          .sortBy(_._1.toString)
          .map { case (a, i) => s"$a -> $i"}
          .mkString("Dups(", ", ", ")")
      s"$as\n$dups"
    })
}


sealed abstract class Prop[A] {
  def unary_~                                : Prop[A] = Negation(this)
  def |                        (q: Prop[A])  : Prop[A] = Disjunction(NonEmptyList(q, this))
  def &                        (q: Prop[A])  : Prop[A] = Conjunction(NonEmptyList(q, this))
  def ==>                      (c: Prop[A])  : Prop[A] = Implication(this, c)
  def <==                      (a: Prop[A])  : Prop[A] = Reduction(this, a)
  def <==>                     (q: Prop[A])  : Prop[A] = Biconditional(this, q)

  def rename(n: String): Prop[A] = Rename(n, this)

  def contramap[Z](f: Z => A): Prop[Z] = Contramap(this, f)

  def forall[Z, F[_] : Foldable](f: Z => F[A]): Prop[Z] = Forall(this, f, true)

  def forallF[F[_] : Foldable] = forall[F[A], F](f => f)

  @inline final def subst[B <: A]: Prop[B] = contramap(a => a: B)

  @inline final def ∨                   (q: Prop[A])   = this | q
  @inline final def ∧                   (q: Prop[A])   = this & q
  @inline final def ⇐                   (a: Prop[A])   = this <== a
  @inline final def ⇔                   (q: Prop[A])   = this <==> q
  @inline final def iff                 (q: Prop[A])   = this <==> q
  @inline final def or                  (q: Prop[A])   = this | q
  @inline final def and                 (q: Prop[A])   = this & q
  @inline final def implies             (c: Prop[A])   = this ==> c
  @inline final def ∀[Z, F[_]: Foldable](f: Z => F[A]) = forall(f)

  def test(a: A): Boolean

  final def falsify(a: A) = falsifyE(a, true)

  @elidable(elidable.ASSERTION)
  final def assert(a: A): Unit =
    falsify(a).foreach(f => {
      val err = f.report
      val sep = "=" * 120
      System.err.println(sep)
      System.err.println(err)
      System.err.println(sep)
      throw new java.lang.AssertionError(err)
    })

  protected[prop] def fmta: A => Any

  def falsifyE: (A, Boolean) => Option[Falsification[A]]

  @inline protected final def falsifyX(f: (A, Boolean) => List[Falsification[A]]): (A, Boolean) => Option[Falsification[A]] =
    (a,e) => if (test(a) == e) None else Some(Falsification(this, f(a,e), Set(fmta(a))))

  @inline protected final def falsifyN =
    falsifyX((a,e) => Nil)

  @inline protected final def falsifyB(ps: NonEmptyList[Prop[A]]) =
    falsifyX((a, e) => ps.list.flatMap(_.falsifyE(a, e)).toList)

  @inline protected final def falsifyP(p: Prop[A], e: Boolean) =
    falsifyX((a, e2) => p.falsifyE(a, e == e2).toList)
}


final case class Atom[A](name: String, t: A => Boolean, fmta: A => Any) extends Prop[A] {
  override def test(a: A) = t(a)
  override def falsifyE = falsifyN
  override def toString = name
}


final case class Negation[A](p: Prop[A]) extends Prop[A] {
  override def unary_~ = p
  override def test(a: A) = !p.test(a)
  override def falsifyE = falsifyN //falsifyP(p, false)
  override def toString = s"¬$p"
  override protected[prop] def fmta = p.fmta
}


final case class Disjunction[A](ps: NonEmptyList[Prop[A]]) extends Prop[A] {
  override def |(q: Prop[A]) = Disjunction(q <:: ps)
  override def test(a: A) = ps.stream.exists(_.test(a))
  override def falsifyE = falsifyN //falsifyB(ps)
  override def toString = ps.stream.map(_.toString).mkString(" ∨ ")
  override protected[prop] def fmta = identity
}

final case class Conjunction[A](ps: NonEmptyList[Prop[A]]) extends Prop[A] {
  override def &(q: Prop[A]) = Conjunction(q <:: ps)
  override def test(a: A) = ps.stream.forall(_.test(a))
  override def falsifyE = falsifyB(ps)
  override def toString = ps.stream.map(_.toString).mkString(" ∧ ")
  override protected[prop] def fmta = identity
}


final case class Implication[A](a: Prop[A], c: Prop[A]) extends Prop[A] {
  override def test(i: A) = !a.test(i) || c.test(i)
  override def falsifyE = falsifyP(c, true)
  override def toString = s"$a ⇒ $c"
  override protected[prop] def fmta = identity
}


final case class Reduction[A](c: Prop[A], a: Prop[A]) extends Prop[A]  {
  override def test(i: A) = !a.test(i) || c.test(i)
  override def falsifyE = falsifyP(c, true)
  override def toString = s"$c ⇐ $a"
  override protected[prop] def fmta = identity
}


final case class Biconditional[A](p: Prop[A], q: Prop[A]) extends Prop[A]  {
  override def test(a: A) = p.test(a) == q.test(a)
  override def falsifyE = falsifyN
  override def toString = s"$p ⇔ $q"
  override protected[prop] def fmta = identity
}


final case class Rename[A](name: String, p: Prop[A]) extends Prop[A] {
  override def rename(n: String): Prop[A] = Rename(n, p)
  override def test(a: A) = p.test(a)
  override def falsifyE = (a,e) => p.falsifyE(a,e).map(_.copy(this))
  override def toString = name
  override protected[prop] def fmta = p.fmta
}


final case class Contramap[A, B](p: Prop[B], f: A => B) extends Prop[A] {
  override def contramap[Z](g: Z => A): Prop[Z] = Contramap(p, f compose g)
  override def test(a: A) = p.test(f(a))
  override def falsifyE = (a,e) => p.falsifyE(f(a), e).map(_.map(_ contramap f))
  override def toString = p.toString
  override protected[prop] def fmta = p.fmta compose f
}


final case class Forall[F[_]: Foldable, A, B](p: Prop[B], f: A => F[B], updName: Boolean) extends Prop[A] {
  override def test(a: A) = f(a).∀(b => p.test(b))
  override def falsifyE = (a, e) =>
    f(a).foldl(List.empty[Falsification[B]])(q => b => p.falsifyE(b, e).toList ::: q) match {
      case Nil => None
      case fs@(_ :: _) =>
        val causes = fs
          .foldLeft(Monoidmap.empty[Prop[B], (List[Falsification[B]], Set[Any])])(
            (q, i) => q.add(i.p, (i.cause, i.inputs)))
          .plainMap.toList
          .map { case (p, (cs, is)) =>
            val causes2 = cs
              .foldLeft(Multimap.set[Prop[B], Any])((q, c) => q.addN(c.p, c.inputs))
              .plainMap.toList
              .map { case (p2, is2) => Falsification(p2, Nil, is2) }
            val inputs2 = causes2.foldLeft(Set.empty[Any])(_ ++ _.inputs)
            Falsification(p, causes2, inputs2).map[A](Forall(_, f, false))
          }
        Some(Falsification(this, causes, Set(a)))
    }
  override def toString = if (updName) s"∀{$p}" else p.toString
  override protected[prop] def fmta = identity
}
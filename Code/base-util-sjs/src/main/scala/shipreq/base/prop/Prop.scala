package shipreq.base.prop

import scala.annotation.{elidable, tailrec}
import scalaz.{Foldable, NonEmptyList}
import scalaz.syntax.foldable._
import shipreq.base.util.Util

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

  def test(a: A, x: Ctx): Boolean
  final def test1(a: A) = test(a, Ctx.single)

  final def falsify(a: A, x: Ctx) = falsifyE(a, x, true)
  final def falsify1(a: A) = falsify(a, Ctx.single)

  @elidable(elidable.ASSERTION)
  final def assert1(a: A): Unit =
    falsify1(a).foreach(f => {
      val err = f.report
      val sep = "=" * 120
      System.err.println(sep)
      System.err.println(err)
      System.err.println(sep)
      throw new java.lang.AssertionError(err)
    })

  def falsifyE: (A, Ctx, Boolean) => Option[Falsification[A]]

  @inline protected final def falsifyX(f: (A, Ctx, Boolean) => List[Falsification[A]]): (A, Ctx, Boolean) => Option[Falsification[A]] =
    (a,x,e) => if (test(a,x) == e) None else Some(Falsification(this, f(a,x,e), Set(a)))

  @inline protected final def falsifyN =
    falsifyX((a,x,e) => Nil)

  @inline protected final def falsifyB(ps: NonEmptyList[Prop[A]]) =
    falsifyX((a, x, e) => ps.list.flatMap(_.falsifyE(a, x, e)).toList)

  @inline protected final def falsifyP(p: Prop[A], e: Boolean) =
    falsifyX((a, x, e2) => p.falsifyE(a, x, e == e2).toList)
}


final case class Atom[A](name: String, t: (A, Ctx) => Boolean) extends Prop[A] {
  override def test(a: A, x: Ctx) = t(a, x)
  override def falsifyE = falsifyN
  override def toString = name
}


final case class Contramap[A, B](p: Prop[B], f: A => B) extends Prop[A] {
  override def contramap[Z](g: Z => A): Prop[Z] = Contramap(p, f compose g)
  override def test(a: A, x: Ctx) = p.test(f(a), x)
  override def falsifyE = (a,x,e) => {
    val b = f(a)
    p.falsifyE(b, x, e).map(_.map(_ contramap f).copy(inputs = Set(b)))
  }
  override def toString = p.toString
}


final case class Forall[F[_]: Foldable, A, B](p: Prop[B], f: A => F[B], updName: Boolean) extends Prop[A] {
  override def test(a: A, x: Ctx) = f(a).∀(b => p.test(b, x))
  override def falsifyE = (a, x, e) =>
    f(a).foldl(List.empty[Falsification[B]])(q => b => p.falsifyE(b, x, e).toList ::: q) match {
      case Nil => None
      case fs@(_ :: _) =>
        val causes = fs
          .foldLeft(Map.empty[Prop[B], (List[Falsification[B]], Set[Any])])((q, i) => {
            val (cs, is) = q.getOrElse(i.p, (List.empty[Falsification[B]], Set.empty[Any]))
            q + (i.p ->(cs ++ i.cause, is ++ i.inputs))
          })
          .toList
          .map { case (p, (cs, is)) =>
            val causes2 = cs
              .foldLeft(Map.empty[Prop[B], Set[Any]])((q, c) =>
                q + (c.p -> (q.getOrElse(c.p, Set.empty[Any]) ++ c.inputs)))
              .toList.map { case (p2, is2) => Falsification(p2, Nil, is2) }
            val inputs2 = causes2.foldLeft(Set.empty[Any])(_ ++ _.inputs)
            Falsification(p, causes2, inputs2).map[A](Forall(_, f, false))
          }
        Some(Falsification(this, causes, Set(a)))
    }
  override def toString = if (updName) s"∀{$p}" else p.toString
}


final case class Rename[A](name: String, p: Prop[A]) extends Prop[A] {
  override def rename(n: String): Prop[A] = Rename(n, p)
  override def test(a: A, x: Ctx) = p.test(a, x)
  override def falsifyE = (a,b,c) => p.falsifyE(a,b,c).map(_.copy(this))
  override def toString = name
}


final case class Negation[A](p: Prop[A]) extends Prop[A] {
  override def unary_~ = p
  override def test(a: A, x: Ctx) = !p.test(a, x)
  override def falsifyE = falsifyN //falsifyP(p, false)
  override def toString = s"¬$p"
}


final case class Disjunction[A](ps: NonEmptyList[Prop[A]]) extends Prop[A] {
  override def |(q: Prop[A]) = Disjunction(q <:: ps)
  override def test(a: A, x: Ctx) = ps.stream.exists(_.test(a, x))
  override def falsifyE = falsifyB(ps)
  override def toString = ps.stream.map(_.toString).mkString(" ∨ ")
}

final case class Conjunction[A](ps: NonEmptyList[Prop[A]]) extends Prop[A] {
  override def &(q: Prop[A]) = Conjunction(q <:: ps)
  override def test(a: A, x: Ctx) = ps.stream.forall(_.test(a, x))
  override def falsifyE = falsifyB(ps)
  override def toString = ps.stream.map(_.toString).mkString(" ∧ ")
}


final case class Implication[A](a: Prop[A], c: Prop[A]) extends Prop[A] {
  override def test(i: A, x: Ctx) = !a.test(i, x) || c.test(i, x)
  override def falsifyE = falsifyP(c, true)
  override def toString = s"$a ⇒ $c"
}


final case class Reduction[A](c: Prop[A], a: Prop[A]) extends Prop[A]  {
  override def test(i: A, x: Ctx) = !a.test(i, x) || c.test(i, x)
  override def falsifyE = falsifyP(c, true)
  override def toString = s"$c ⇐ $a"
}


final case class Biconditional[A](p: Prop[A], q: Prop[A]) extends Prop[A]  {
  override def test(a: A, x: Ctx) = p.test(a, x) == q.test(a, x)
  override def falsifyE = falsifyN // TODO different than disjunction, breakdown or not?
  override def toString = s"$p ⇔ $q"
}


object Prop {

  @inline final def apply[A](name: String, t: A => Boolean): Prop[A] =
    withCtx(name, (a,_) => t(a))

  @inline final def withCtx[A](name: String, t: (A, Ctx) => Boolean): Prop[A] =
    new Atom[A](name, t)
}

final case class Falsification[A](p: Prop[A], cause: List[Falsification[A]], inputs: Set[Any]) {

  def map[B](f: Prop[A] => Prop[B]): Falsification[B] =
    Falsification(f(p), cause map (_ map f), inputs)

  // TODO rootCauses is redundant now or what?
  def rootCauses: NonEmptyList[Prop[A]] = {
    @tailrec
    def loop2(fs: List[Falsification[A]], cs: List[Prop[A]]): List[Prop[A]] =
      fs match {
        case Nil => cs
        case h :: t => loop2(t, loop(h, cs).list)
      }
    @tailrec
    def loop(f: Falsification[A], cs: List[Prop[A]]): NonEmptyList[Prop[A]] =
      f match {
        case Falsification(p, Nil, _)    => NonEmptyList.nel(p, cs.filterNot(_ == p))
        case Falsification(_, h :: t, _) => loop(h, loop2(t, cs))
      }
    loop(this, Nil)
  }

  def rootCausesAndInputs: Map[Prop[A], Set[Any]] = {
    type R = Map[Prop[A], Set[Any]]
    @inline def addR(r: R, p: Prop[A], i: Set[Any]): R =
      r + (p -> (r.getOrElse(p, Set.empty[Any]) ++ i))
    def loop(f: Falsification[A], r: R): R =
      f match {
        case Falsification(p, Nil, i)        => addR(r, p, i)
        case Falsification(_, c@(_ :: _), _) => c.foldLeft(r)((q,c) => loop(c, q))
      }
    loop(this, Map.empty)
  }

  def failureTree = failureTreeI("")
  def failureTreeI(indent: String): String = Util.quickSB(failureTreeSB(_, indent))
  def failureTreeSB(sb: StringBuilder, indent: String = ""): Unit =
    AsciiTree[Falsification[A]](sb, List(this), _.p.toString, _.cause, indent)

  def rootCauseTree = rootCauseTreeI("")
  def rootCauseTreeI(indent: String): String = Util.quickSB(rootCauseTreeSB(_, indent))
  def rootCauseTreeSB(sb: StringBuilder, indent: String = ""): Unit = {
    val m = rootCausesAndInputs
    trait X
    case class K(k: Prop[A]) extends X {
      override val toString = k.toString
    }
    case class I(i: Any) extends X {
      override val toString = i.toString
    }
    case object T extends X {
      override def toString = s"${m.size} props, ${m.values.foldLeft(Set.empty[Any])(_ ++ _).size} inputs."
    }
    val keys = m.keys.toList.map(K).sortBy(_.toString)
    AsciiTree[X](sb, List(T), _.toString, {
      case T    => keys
      case K(k) => m(k).map(I).toList.sortBy(_.toString)
      case I(_) => Nil
    }, indent)
  }

  def report = {
    def is = inputs.toList.sortBy(_.toString) match {
      case Nil        => "."
      case h :: Nil   => s" on\ninput: $h"
      case l@(_ :: _) => s" on\ninputs: ${l.mkString("\n        ")}"
    }
    s"Property [$p] failed$is" +
      s"\n\nRoot causes:\n${rootCauseTreeI("  ")}" +
      s"\n\nFailure tree:\n${failureTreeI("  ")}"
  }
}

object AsciiTree {
  def apply[N](sb: StringBuilder, root: List[N], show: N => String, leaves: N => List[N], indent: String = ""): Unit = {
    val pm = "│  "
    val pl = "   "
    val cm = "├─ "
    val cl = "└─ "
    var first = true
    def loop(parentLvlLast: Vector[Boolean], fs: List[N], root: Boolean): Unit = fs match {
      case Nil =>
      case h :: t =>
        if (first) first = false else sb append '\n'
        sb append indent
        for (b <- parentLvlLast) sb.append(if (b) pl else pm)
        val last = t.isEmpty
        if (!root) sb.append(if (last) cl else cm)
        sb append show(h)
        val nextLvl = if (root) Vector.empty[Boolean] else parentLvlLast :+ last
        loop(nextLvl, leaves(h), false)
        loop(parentLvlLast, t, root)
    }
    loop(Vector.empty, root, true)
  }
}
package shipreq.webapp.member.project.formula

import cats.{Applicative, Traverse}
import cats.instances.list._
import cats.syntax.all._
import japgolly.microlibs.recursion.Fix
import shipreq.base.util.TraverseWithDefaults

sealed trait FormulaAst[+Self, +Fn, +Field]

object FormulaAst {

  final case class Value(value: FormulaValue) extends FormulaAst[Nothing, Nothing, Nothing]

  final case class Add     [+A](lhs: A, rhs: A) extends FormulaAst[A, Nothing, Nothing]
  final case class Subtract[+A](lhs: A, rhs: A) extends FormulaAst[A, Nothing, Nothing]
  final case class Divide  [+A](lhs: A, rhs: A) extends FormulaAst[A, Nothing, Nothing]
  final case class Multiply[+A](lhs: A, rhs: A) extends FormulaAst[A, Nothing, Nothing]

  final case class Compare[+A](lhs: A, op: FormulaCmpOp, rhs: A) extends FormulaAst[A, Nothing, Nothing]

  final case class Function[+Fn, +A](function: Fn, args: List[A]) extends FormulaAst[A, Fn, Nothing]

  final case class Field[+F](field: F) extends FormulaAst[Nothing, Nothing, F]

  // ===================================================================================================================

  type Fixed[A, B] = Fix[λ[X => FormulaAst[X, A, B]]]

  implicit def univEqFix[A: UnivEq, B: UnivEq]: UnivEq[Fixed[A, B]] =
    UnivEq.deriveFix[Fix, λ[X => FormulaAst[X, A, B]]]

  implicit def traverse[Fn, Field]: Traverse[FormulaAst[*, Fn, Field]] =
    new TraverseWithDefaults[FormulaAst[*, Fn, Field]] {
      type F[A] = FormulaAst[A, Fn, Field]

      override def map[A, B](fa: F[A])(f: A => B): F[B] = fa match {
        case v: Value              => v
        case Add(lhs, rhs)         => Add(f(lhs), f(rhs))
        case Subtract(lhs, rhs)    => Subtract(f(lhs), f(rhs))
        case Multiply(lhs, rhs)    => Multiply(f(lhs), f(rhs))
        case Divide(lhs, rhs)      => Divide(f(lhs), f(rhs))
        case Compare(lhs, op, rhs) => Compare(f(lhs), op, f(rhs))
        case Function(fn, args)    => Function(fn, args map f)
        case f@ Field(_)           => f
      }

      override def traverse[G[_], A, B](fa: F[A])(f: A => G[B])(implicit G: Applicative[G]): G[F[B]] = fa match {
        case v: Value              => G pure v
        case Add(lhs, rhs)         => G.map2(f(lhs), f(rhs))(Add(_, _))
        case Subtract(lhs, rhs)    => G.map2(f(lhs), f(rhs))(Subtract(_, _))
        case Multiply(lhs, rhs)    => G.map2(f(lhs), f(rhs))(Multiply(_, _))
        case Divide(lhs, rhs)      => G.map2(f(lhs), f(rhs))(Divide(_, _))
        case Compare(lhs, op, rhs) => G.map2(f(lhs), f(rhs))(Compare(_, op, _))
        case Function(fn, args)    => G.map(args traverse f)(Function(fn, _))
        case f@ Field(_)           => G pure f
      }
    }

  // ===================================================================================================================

  trait Dsl {
    type Fn
    type Field

    final type F[A] = FormulaAst[A, Fn, Field]

    def apply(f: F[Fix[F]]): Fix[F] =
      Fix[F](f)

    def value(v: FormulaValue): Fix[F] =
      Fix[F](Value(v))

    def lit(b: Boolean): Fix[F] = value(FormulaValue.Bool(b))
    def lit(d: Double) : Fix[F] = value(FormulaValue.Dbl(d))
    def lit(s: String) : Fix[F] = value(FormulaValue.Str(s))

    def add(lhs: Fix[F], rhs: Fix[F]): Fix[F] =
      Fix[F](Add(lhs, rhs))

    def subtract(lhs: Fix[F], rhs: Fix[F]): Fix[F] =
      Fix[F](Subtract(lhs, rhs))

    def divide(lhs: Fix[F], rhs: Fix[F]): Fix[F] =
      Fix[F](Divide(lhs, rhs))

    def multiply(lhs: Fix[F], rhs: Fix[F]): Fix[F] =
      Fix[F](Multiply(lhs, rhs))

    def compare(lhs: Fix[F], op: FormulaCmpOp, rhs: Fix[F]): Fix[F] =
      Fix[F](Compare(lhs, op, rhs))

    def function(fn: Fn, args: List[Fix[F]]): Fix[F] =
      Fix[F](Function(fn, args))

    def field(f: Field): Fix[F] =
      Fix[F](Field(f))
  }

}

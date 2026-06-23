package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion.Fix

sealed trait FormulaAst[+Self]

object FormulaAst {

  final case class Value(value: FormulaValue) extends FormulaAst[Nothing]

  final case class Add     [+A](lhs: A, rhs: A) extends FormulaAst[A]
  final case class Subtract[+A](lhs: A, rhs: A) extends FormulaAst[A]
  final case class Divide  [+A](lhs: A, rhs: A) extends FormulaAst[A]
  final case class Multiply[+A](lhs: A, rhs: A) extends FormulaAst[A]

  final case class Compare[+A](lhs: A, op: FormulaCmpOp, rhs: A) extends FormulaAst[A]

  // ===================================================================================================================

  type Fixed = Fix[λ[X => FormulaAst[X]]]

  implicit def univEqFix: UnivEq[Fixed] =
    UnivEq.deriveFix[Fix, λ[X => FormulaAst[X]]]

  // ===================================================================================================================

  trait Dsl {
    final type F[A] = FormulaAst[A]

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
  }

}

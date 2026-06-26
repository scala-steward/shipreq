package shipreq.webapp.member.project.formula

import cats.instances.list._
import cats.syntax.traverse._
import japgolly.microlibs.recursion._
import java.math.{BigDecimal, RoundingMode}
import java.util.regex.Pattern
import shipreq.base.util.ErrorMsg
import shipreq.webapp.member.project.data.DataImplicits._
import shipreq.webapp.member.project.data.{FieldSet, Req, ReqData}
import shipreq.webapp.member.project.filter.FilterAlgebra

/** Algebras:
  *
  * {{{
  *   unparse   : FAlgebra [               PotentialF, AtomOrComposite[String]]
  *   validate  : FAlgebraM[ErrorMsg \/ *, PotentialF, Valid]
  *   unvalidate: FAlgebra [               ValidF,     Potential]
  *   eval      : FAlgebraM[ErrorMsg \/ *, ValidF,     FormulaValue]
  * }}}
  */
object FormulaAlgebra {
  import Formula._
  import FormulaAst._
  import FormulaValue._

  val isFieldNameUnquotedChar: Char => Boolean = {
    case '(' | ')' | ',' | '+' | '-' | '*' | '/' => false
    case c => FilterAlgebra.isFieldNameUnquotedChar(c)
  }

  def quoteFieldName(name: String): String =
    if (name.forall(isFieldNameUnquotedChar))
      name
    else
      "\"" + name + "\""

  @inline private def fail(err: String) = -\/(ErrorMsg(err))

  private val trailingZeros = Pattern.compile("\\.0+$")

  private def doubleToString(d: Double): String =
    trailingZeros.matcher(d.toString).replaceFirst("")

  // ===================================================================================================================

  val unparse: FAlgebra[PotentialF, AtomOrComposite[String]] = {
    import shipreq.base.util.SafeStringOps._
    import AtomOrComposite.string._
    implicit def autoAtom(s: String) = atom(s)
    implicit final class AtomOrCompositeExt(aoc: AtomOrComposite[String]) {
      @inline def noParens: String = aoc match {
        case AtomOrComposite.Atom(a) => a
        case AtomOrComposite.Composite(c, _) => c
      }
    }

    {
      case Value(Empty)       => ""
      case Value(Bool(true))  => "true"
      case Value(Bool(false)) => "false"
      case Value(Dbl(d))      => doubleToString(d)
      case Value(Str(s))      => '"' ~ s.replace("\"", "\"\"") ~ '"'
      case Add(l, r)          => composite("(", l.atom ~ " + " ~ r.atom, ")")
      case Subtract(l, r)     => composite("(", l.atom ~ " - " ~ r.atom, ")")
      case Multiply(l, r)     => composite("(", l.atom ~ " * " ~ r.atom, ")")
      case Divide(l, r)       => composite("(", l.atom ~ " / " ~ r.atom, ")")
      case Field(f)           => "field:" ~ quoteFieldName(f)
      case Compare(l, op, r)  => composite("(", l.noParens ~ ' ' ~ op.symbol ~ ' ' ~ r.atom, ")")
      case Function(f, args)  => f.toUpperCase ~ '(' ~ args.iterator.map(_.noParens).mkString(", ") ~ ')'
    }
  }

  // ===================================================================================================================

  def validate(fields: FieldSet): FAlgebraM[ErrorMsg \/ *, PotentialF, Valid] = {

    case Function(name, args) =>
      FormulaFunction.byName.get(name) match {
        case Some(f) =>
          val a = args.size
          def validWhen(argCheck: Boolean) =
            if (argCheck)
              \/-(Valid.function(f, args))
            else
              fail("Invalid number of args for function: " + name)

          f match {
            case FormulaFunction.And      => validWhen(true)
            case FormulaFunction.Average  => validWhen(a > 0)
            case FormulaFunction.Ceiling  => validWhen(a == 1)
            case FormulaFunction.Floor    => validWhen(a == 1)
            case FormulaFunction.If       => validWhen(a == 2 || a == 3)
            case FormulaFunction.IsBlank  => validWhen(a == 1)
            case FormulaFunction.IsBool   => validWhen(a == 1)
            case FormulaFunction.IsNumber => validWhen(a == 1)
            case FormulaFunction.IsText   => validWhen(a == 1)
            case FormulaFunction.Max      => validWhen(a > 0)
            case FormulaFunction.Min      => validWhen(a > 0)
            case FormulaFunction.Not      => validWhen(a == 1)
            case FormulaFunction.Or       => validWhen(true)
            case FormulaFunction.Round    => validWhen(a == 1 || a == 2)
          }

        case None =>
          fail("Unknown function: " + name)
      }

    case Field(name) =>
      fields.customNumberFields.find(_.name ==* name) match {
        case Some(f) => \/-(Valid.field(FormulaFieldRef.NumberField(f.id)))
        case None    => fail("Invalid field: " + name)
      }

    case x: Value            => \/-(Valid(x))
    case x@ Add(_, _)        => \/-(Valid(x))
    case x@ Subtract(_, _)   => \/-(Valid(x))
    case x@ Multiply(_, _)   => \/-(Valid(x))
    case x@ Divide(_, _)     => \/-(Valid(x))
    case x@ Compare(_, _, _) => \/-(Valid(x))
  }

  // ===================================================================================================================

  def unvalidate(fieldSet: FieldSet): FAlgebra[ValidF, Potential] = {
    case v: Value            => Potential(v)
    case x@ Add(_, _)        => Potential(x)
    case x@ Subtract(_, _)   => Potential(x)
    case x@ Multiply(_, _)   => Potential(x)
    case x@ Divide(_, _)     => Potential(x)
    case x@ Compare(_, _, _) => Potential(x)
    case Function(f, args)   => Potential(Function(f.name, args))

    case Field(ref) =>
      val name: String =
        ref match {
          case FormulaFieldRef.NumberField(fid) => fieldSet.custom(fid).name
        }
      Potential(Field(name))
  }

  // ===================================================================================================================

  private val typeMismatch = fail("Type mismatch.")
  private val invalidNumberOfFnArgs = fail("Arg count mismatch.") // this should be caught at the validation stage

  def eval(fieldSet: FieldSet, reqNums: ReqData.Numbers, req: Req): FAlgebraM[ErrorMsg \/ *, ValidF, FormulaValue] = {
    import FormulaValue._
    type Res = ErrorMsg \/ FormulaValue

    {
      case Value(v) => \/-(v)

      case Add(lhs, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y))  => \/-(Dbl(x + y))
          case (Str(x), Str(y))  => \/-(Str(x + y))
          case (Str(x), Dbl(y))  => \/-(Str(x + doubleToString(y)))
          case (Str(x), Bool(y)) => \/-(Str(x + y.toString.toUpperCase))
          case (x: Str, Empty)   => \/-(x)
          case _                 => typeMismatch
        }

      case Subtract(lhs, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y)) => \/-(Dbl(x - y))
          case _                => typeMismatch
        }

      case Multiply(lhs, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y)) => \/-(Dbl(x * y))
          case (Str(x), Dbl(y)) => \/-(Str(x * (y + 0.5).toInt))
          case _                => typeMismatch
        }

      case Divide(lhs, rhs) =>
        (lhs, rhs) match {
          case (Dbl(_), Dbl(0)) => fail("Division by zero.")
          case (Dbl(x), Dbl(y)) => \/-(Dbl(x / y))
          case _                => typeMismatch
        }

      case Compare(lhs, FormulaCmpOp.`=`, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y))   => \/-(Bool(x ==* y))
          case (Str(x), Str(y))   => \/-(Bool(x ==* y))
          case (Bool(x), Bool(y)) => \/-(Bool(x ==* y))
          case (Empty, Empty)     => \/-(Bool(true))
          case _                  => \/-(Bool(false))
        }

      case Compare(lhs, FormulaCmpOp.!=, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y))   => \/-(Bool(x !=* y))
          case (Str(x), Str(y))   => \/-(Bool(x !=* y))
          case (Bool(x), Bool(y)) => \/-(Bool(x !=* y))
          case (Empty, Empty)     => \/-(Bool(false))
          case _                  => \/-(Bool(true))
        }

      case Compare(lhs, FormulaCmpOp.<, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y)) => \/-(Bool(x < y))
          case _                => typeMismatch
        }

      case Compare(lhs, FormulaCmpOp.>, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y)) => \/-(Bool(x > y))
          case _                => typeMismatch
        }

      case Compare(lhs, FormulaCmpOp.<=, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y)) => \/-(Bool(x <= y))
          case _                => typeMismatch
        }

      case Compare(lhs, FormulaCmpOp.>=, rhs) =>
        (lhs, rhs) match {
          case (Dbl(x), Dbl(y)) => \/-(Bool(x >= y))
          case _                => typeMismatch
        }

      case Field(ref) => ref match {

        case FormulaFieldRef.NumberField(fid) =>
          val f = fieldSet.custom(fid)
          reqNums.getVirtual(f, req) match {
            case Some(d) => \/-(Dbl(d))
            case None    => \/-(Empty)
          }
      }

      case Function(fn, args) =>
        def foldBoolArgs(start: Boolean)(f: (Boolean, Boolean) => Boolean): Res =
          args.foldLeft[Res](\/-(Bool(start)))((r, bv) =>
            r.flatMap(av =>
              (av, bv) match {
                case (Bool(a), Bool(b)) => \/-(Bool(f(a, b)))
                case _                  => typeMismatch
              }
            )
          )

        def reduceDbls(f: (Double, Double) => Double): Res =
          if (args.isEmpty)
            invalidNumberOfFnArgs
          else
            args.traverse {
              case Dbl(d) => \/-(d)
              case _      => typeMismatch
            }.map(doubles => Dbl(doubles.reduce(f)))

        def isType(f: PartialFunction[FormulaValue, Unit]): Res =
          args match {
            case arg :: Nil => \/-(Bool(f.isDefinedAt(arg)))
            case _ => invalidNumberOfFnArgs
          }

        fn match {
          case FormulaFunction.And =>
            foldBoolArgs(true)(_ && _)

          case FormulaFunction.Average =>
            reduceDbls(_ + _).flatMap {
              case Dbl(d) => \/-(Dbl(d / args.size))
              case _      => typeMismatch
            }

          case FormulaFunction.Ceiling =>
            args match {
              case arg :: Nil => arg match {
                case Dbl(d) => \/-(Dbl(Math.ceil(d)))
                case _       => typeMismatch
              }
              case _ => invalidNumberOfFnArgs
            }

          case FormulaFunction.Floor =>
            args match {
              case arg :: Nil => arg match {
                case Dbl(d) => \/-(Dbl(Math.floor(d)))
                case _       => typeMismatch
              }
              case _ => invalidNumberOfFnArgs
            }

          case FormulaFunction.If =>
            @inline def ifThen(b: Boolean, x: FormulaValue, y: FormulaValue): FormulaValue =
              if (b) x else y
            args match {
              case arg1 :: arg2 :: Nil => (arg1, arg2) match {
                case (Bool(b), x) => \/-(ifThen(b, x, Empty))
                case _            => typeMismatch
              }
              case arg1 :: arg2 :: arg3 :: Nil => (arg1, arg2, arg3) match {
                case (Bool(b), x, y) => \/-(ifThen(b, x, y))
                case _               => typeMismatch
              }
              case _ => invalidNumberOfFnArgs
            }

          case FormulaFunction.IsBlank =>
            isType { case Empty => }

          case FormulaFunction.IsBool =>
            isType { case _: Bool => }

          case FormulaFunction.IsNumber =>
            isType { case _: Dbl => }

          case FormulaFunction.IsText =>
            isType { case _: Str => }

          case FormulaFunction.Max =>
            reduceDbls(Math.max)

          case FormulaFunction.Min =>
            reduceDbls(Math.min)

          case FormulaFunction.Not =>
            args match {
              case arg :: Nil => arg match {
                case Bool(b) => \/-(Bool(!b))
                case _       => typeMismatch
              }
              case _ => invalidNumberOfFnArgs
            }

          case FormulaFunction.Or =>
            foldBoolArgs(false)(_ || _)

          case FormulaFunction.Round =>
            def round(d: Double, scale: Double): Double =
              new BigDecimal(d).setScale(scale.toInt, RoundingMode.HALF_UP).doubleValue
            args match {
              case arg :: Nil => arg match {
                case Dbl(d) => \/-(Dbl(round(d, 0)))
                case _      => typeMismatch
              }
              case arg1 :: arg2 :: Nil => (arg1, arg2) match {
                case (Dbl(d), Dbl(s)) => \/-(Dbl(round(d, s)))
                case _                => typeMismatch
              }
              case _ => invalidNumberOfFnArgs
            }
        }
    }
  }
}

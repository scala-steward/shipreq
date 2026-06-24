package shipreq.webapp.member.project.formula

import japgolly.microlibs.recursion._
import java.util.regex.Pattern
import shipreq.base.util.ErrorMsg
import shipreq.webapp.member.project.data.FieldSet
import shipreq.webapp.member.project.filter.FilterAlgebra

/** Algebras:
  *
  * {{{
  *   unparse : FAlgebra [               PotentialF, AtomOrComposite[String]]
  *   validate: FAlgebraM[ErrorMsg \/ *, PotentialF, Valid]
  * }}}
  */
object FormulaAlgebra {
  import Formula._
  import FormulaAst._
  import FormulaValue._

  @inline def isFieldNameUnquotedChar: Char => Boolean =
    FilterAlgebra.isFieldNameUnquotedChar

  @inline def quoteFieldName(name: String): String =
    FilterAlgebra.quoteFieldName(name)

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
    val trailingZeros = Pattern.compile("\\.0+$")

    {
      case Value(Bool(true))  => "true"
      case Value(Bool(false)) => "false"
      case Value(Dbl(d))      => trailingZeros.matcher(d.toString).replaceFirst("")
      case Value(Str(s))      => '"' ~ s ~ '"'
      case Add(l, r)          => composite("(", l.atom ~ " + " ~ r.atom, ")")
      case Subtract(l, r)     => composite("(", l.atom ~ " - " ~ r.atom, ")")
      case Multiply(l, r)     => composite("(", l.atom ~ " * " ~ r.atom, ")")
      case Divide(l, r)       => composite("(", l.atom ~ " / " ~ r.atom, ")")
      case Field(f)           => "field:" ~ quoteFieldName(f)
      case Compare(l, op, r)  => l.noParens ~ ' ' ~ op.symbol ~ ' ' ~ r.noParens
      case Function(f, args)  => f.toUpperCase ~ '(' ~ args.iterator.map(_.noParens).mkString(", ") ~ ')'
    }
  }

  // ===================================================================================================================

  def validate(fields: FieldSet): FAlgebraM[ErrorMsg \/ *, PotentialF, Valid] = {
    @inline def fail(err: String) = -\/(ErrorMsg(err))

    {
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
              case FormulaFunction.If    => validWhen(a == 2 || a == 3)
              case FormulaFunction.Not   => validWhen(a == 1)
              case FormulaFunction.Round => validWhen(a == 1 || a == 2)
            }

          case None =>
            fail("Unknown function: " + name)
        }

      case Field(name) =>
        fields.customNumberFields.find(_.name ==* name) match {
          case Some(f) => \/-(Valid.field(\/-(f.id)))
          case None    => fail("Invalid field: " + name)
        }

      case x: Value            => \/-(Valid(x))
      case x@ Add(_, _)        => \/-(Valid(x))
      case x@ Subtract(_, _)   => \/-(Valid(x))
      case x@ Multiply(_, _)   => \/-(Valid(x))
      case x@ Divide(_, _)     => \/-(Valid(x))
      case x@ Compare(_, _, _) => \/-(Valid(x))
    }
  }
}

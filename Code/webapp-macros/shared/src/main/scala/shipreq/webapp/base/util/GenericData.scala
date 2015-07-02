package shipreq.webapp.base.util

import monocle.Lens
import shipreq.base.util.{IMap => IM, NonEmptySet, UnivEq}

abstract class GenericData[Data] {

  /**
   * An attribute of [[Data]].
   */
  trait AttrBase {
    this: Attr =>
    type A
    val lens: monocle.Lens[Data, A]
    def value(a: A): ValueFor[this.type]
  }

  type Attr <: AttrBase

  /**
   * A value and the attribute to which it applies.
   */
  trait ValueBase {
    this: Value =>
    val attr: Attr
    val value: attr.A
  }

  type Value <: ValueBase

  type ValueFor[A <: Attr] = Value {val attr: A}

  implicit def equality: UnivEq[Attr]

  val attrs: NonEmptySet[Attr]

  type IMap = IM[Attr, Value]

  def emptyIMap: IMap = IM.empty(_.attr)
}

// =====================================================================================================================
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

@compileTimeOnly("Enable macro paradise to expand macro annotations")
class GenericDataAttrs(lenses: Lens[_, _]*) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenericDataMacros.objectImpl
}

object GenericDataMacros {
  import shipreq.webapp.macros.MacroUtils._
  import shipreq.webapp.macros.WhiteboxMacroUtils._
  import scala.reflect.macros.whitebox.Context

  def objectImpl(c: Context)(annottees: c.Expr[Any]*) = {
    import c.universe._

    val args = extractStaticAnnotationArgs(c)
    if (args.isEmpty)
      fail(c, "No attributes specified.")

    val (attrNames, defns) = args.map { arg =>
      val name = arg match {
        case Select(_, n) => n
        case x => fail(c, s"Unable to extract attribute name from: $x")
      }

      val prefix: String = {
        val n = name.toTermName.decodedName.toString
        n.head.toString.toUpperCase + n.tail
      }

      val attrName   = prefix
      val attrNameT  = TermName(attrName)
      val valueName  = prefix + "Value"
      val valueNameT = TermName(valueName)
      val valueNameY = TypeName(valueName)

      val defn =
        q"""
          case object $attrNameT extends AttrA($arg) {
            override def value(a: A) = $valueNameT(a)
          }
          final case class $valueNameY(value: $attrNameT.A) extends Value {
            override val attr: $attrNameT.type = $attrNameT
          }
        """

      (attrNameT, defn)
    }.unzip

    val impl =
      annottees.map(_.tree) match {
        case List(q"object $objName extends $parent[$t] { ..$body }") if body.isEmpty =>
          val T = t

          q"""
            object $objName extends $parent[$T] {
              import shipreq.base.util.{IMap => IM, NonEmptySet, UnivEq}

              sealed trait Attr extends AttrBase

              sealed abstract class AttrA[_A](override final val lens: monocle.Lens[$T, _A]) extends Attr {
                final override type A = _A
              }

              sealed trait Value extends ValueBase

              ..${flattenBlocks(c)(defns)}

              override implicit def equality: UnivEq[Attr] = UnivEq.force

              override val attrs: NonEmptySet[Attr] = NonEmptySet(..$attrNames)
            }
           """

        case _ => fail(c, "You must annotate an object definition with an empty body.")
      }

//    val sep = ("="*120)+"\n"
//    println(sep + impl + "\n" + sep)

    c.Expr[Any](impl)
  }
}
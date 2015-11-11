package shipreq.idea

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScSimpleTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScReferenceExpression, ScGenericCall}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScPatternDefinition, ScValue}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.expr.ScReferenceExpressionImpl
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.{TypeDefinitionMembers, SyntheticMembersInjector}
import org.jetbrains.plugins.scala.lang.psi.types.result.TypingContext

import scala.reflect.ClassTag

object ShipReqMacroExpander {
  implicit class AnyExt[A](private val a: A) extends AnyVal {
    def tryCast[B <: A](implicit c: ClassTag[B]): Option[B] =
      c.unapply(a)
  }

  def fakeCaseClass1(cls: String, arg1Name: String, arg1Type: String, mod: String = "", ext: String = "")(body: String): List[String] =
    s"$mod class $cls(val $arg1Name: $arg1Type) extends ${Option(ext).filter(_.nonEmpty).fold("")(_ + " with")} Product with Serializable {$body}" ::
      s"object $cls{def apply($arg1Name: $arg1Type): $cls = ???; def unapply(x: $cls): Option[$arg1Type] = ???}" ::
      Nil

}
class ShipReqMacroExpander extends SyntheticMembersInjector {
  import ShipReqMacroExpander._

//  override def needsCompanionObject(source: ScTypeDefinition): Boolean =
//    source match {
//      case o: ScObject if o.annotations.exists(_.getQualifiedName == CreateGenericData) => true
//      case _ => false
//    }

  override def injectInners(source: ScTypeDefinition): Seq[String] =
    source match {
      case o: ScObject if o.annotations.exists(_.getQualifiedName == CreateGenericData) => go1(o)
      case _ => Nil
    }

  override def injectFunctions(source: ScTypeDefinition): Seq[String] =
    source match {
      case o: ScObject if o.annotations.exists(_.getQualifiedName == CreateGenericData) => go(o)
      case _ => Nil
    }

  private[this] val CreateGenericData = "shipreq.webapp.base.util.CreateGenericData"

  private def go1(o: ScObject): Seq[String] = {
    val r = List.newBuilder[String]

//    val prefix = o.qualifiedName + "."
    val prefix = ""

    r += s"sealed abstract class Attr extends ${prefix}AttrBase"
    r += s"sealed abstract class Value extends ${prefix}ValueBase"

    println("-" * 120)

    for {
      member <- o.members
      patDef <- member.tryCast[ScPatternDefinition] if patDef.declaredElements.lengthCompare(1) == 0
      call   <- patDef.expr.flatMap(_.tryCast[ScGenericCall])
      refExpr <- call.referencedExpr.tryCast[ScReferenceExpression] if refExpr.refName == "defAttr"
      args   <- call.typeArgs.map(_.typeArgs) if args.lengthCompare(1) == 0
    //argType = args.head.getType(TypingContext.empty)
    } {
      val Type  = args.head.getText
      val Name = patDef.declaredElements.head.name

      val attrNameT = Name.replaceFirst("^_+", "").capitalize
      val attrType = Type
      val valueNameT = "ValueFor" + attrNameT
      val valueNameY = valueNameT

//      println(s"$Name: $Type")

      val x1 = s"""
                  |case object $attrNameT extends Attr {
                  |  override type Data = $attrType
                  |  override def apply(data: Data): $valueNameT = ???
                  |}
                """.stripMargin.trim

      val x2 = fakeCaseClass1(valueNameY, "value", attrType, mod = "final", ext = "Value")(
        s"override def attr: $attrNameT.type = ???")

      r += x1
      r ++= x2
      if (o.toString.contains("ReqCodeGroupGD")) {
        println(x1)
        x2 foreach println
      }
    }

    r.result()
  }

  private def go(o: ScObject): Seq[String] = {
    val r = List.newBuilder[String]
    val prefix = o.qualifiedName + "."

    //    r += "import scalaz.{Equal, Order}"
    //    r += "import shipreq.base.util.{NonEmptySet, UnivEq}"

        r += "override implicit def equalityAttr: scalaz.Order[Attr] with shipreq.base.util.UnivEq[Attr] = ???"
        r += "override implicit def equalityValue: shipreq.base.util.UnivEq[Value] = ???"
        r += "override def attrs: shipreq.base.util.NonEmptySet[Attr] = ???"

//    r += "implicit def equalityAttr = ???"
//    r += "implicit def equalityValue = ???"
//    r += "def attrs = ???"

    //    if (!o.toString.contains("CustomReqTypeGD"))
//    if (true)
//      return

    r.result()
  }
}
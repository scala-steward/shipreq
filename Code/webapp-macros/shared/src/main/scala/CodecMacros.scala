package shipreq.webapp.base.protocol

import upickle.ReadWriter
import scala.reflect.macros.blackbox.Context

object CodecMacros {

  def caseClass[T]: ReadWriter[T] = macro __caseClassImpl[T]

  def __caseClassImpl[T: c.WeakTypeTag](c: Context): c.Expr[ReadWriter[T]] = {
    import c.universe._

    val T = weakTypeOf[T]
    val TC = T.typeSymbol.companion

    def fail(msg: String): Nothing =
      c.abort(c.enclosingPosition, msg)

    def paramType(name: TermName): Type =
      T.decl(name).typeSignatureIn(T) match {
        case NullaryMethodType(t) => t
        case t                    => t
      }

    val params =
      T.decls
        .collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }
        .getOrElse(fail("Unable to discern primary constructor."))
        .paramLists
        .headOption
        .getOrElse(fail("Primary constructor missing paramList."))

    val ReadWriter = Ident(c.mirror staticModule "upickle.ReadWriter")
    val JsArr      = Ident(c.mirror staticModule "upickle.Js.Arr")
    val Fns        = Ident(c.mirror staticModule "upickle.Fns")
    val writeJs    = q"$Fns.writeJs"
    val readJs     = q"$Fns.readJs"

    def invokeWriteJs(param: Symbol) = {
      val a = param.asTerm.name
      val A = paramType(a)
      q"$writeJs(t.$a)"
    }
    def invokeReadJs(vname: TermName, param: Symbol) = {
      val a = param.asTerm.name
      val A = paramType(a)
      q"$readJs[$A]($vname)"
    }

    val impl =
      params match {
        case Nil =>
          fail(s"Class constructor has no parameters.")

        case param :: Nil =>
          val j = TermName("j")
          q"$ReadWriter[$T](t => ${invokeWriteJs(param)}, {case $j => $TC(${invokeReadJs(j, param)})})"

        case _ =>
          val writes = params map (invokeWriteJs(_))
          var nextChar = 'a'.toInt
          val tmp = params map { p =>
            val v = TermName(nextChar.toChar.toString)
            nextChar += 1
            (pq"$v", invokeReadJs(v, p))
          }
          val (vals, reads) = tmp.unzip
          val readCase = cq"$JsArr(..$vals) => $TC(..$reads)"
          q"$ReadWriter[$T](t => $JsArr(..$writes), {case $readCase})"
      }

    //println("\n" + impl + "\n")
    c.Expr[ReadWriter[T]](impl)
  }
}

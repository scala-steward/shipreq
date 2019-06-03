package shipreq.base.util

import java.util.UUID
import scalaz.Functor
import scalaz.syntax.functor._
import scalaz.Scalaz.Id

/** Takes a potentially slow `String* => String` function and makes it super fast by executing it once,
  * turning the result into a template, then using the template for all subsequent calls.
  *
  * This assumes provided functions are pure.
  */
object Template {

  def functor1A[F[_], A](f: A => F[String])(implicit F: Functor[F], p1: Param[A]): F[A => String] =
    prepareF(1, a => f(p1.fromStr(a(0)))).map(t => (a: A) => t(Array(p1.toStr(a))))

  def functor1[F[_]: Functor](f: String => F[String]): F[String => String] =
    prepareF(1, a => f(a(0))).map(t => s => t(Array(s)))

  def apply1(f: String => String): String => String = {
    val t = prepare(1, a => f(a(0)))
    s => t(Array(s))
  }

  def functor2A[F[_], A, B](f: (A, B) => F[String])(implicit F: Functor[F], p1: Param[A], p2: Param[B]): F[(A, B) => String] =
    prepareF(2, a => f(p1.fromStr(a(0)), p2.fromStr(a(1)))).map(t => (a: A, b: B) => t(Array(p1.toStr(a), p2.toStr(b))))

  final class Param[A](val fromStr: String => A, val toStr: A => String)

  object Param {
    def apply[A](fromStr: String => A)(toStr: A => String): Param[A] =
      new Param(fromStr, toStr)

    implicit val string: Param[String] = {
      val id = (s: String) => s
      apply(id)(id)
    }
  }

  def apply2(f: (String, String) => String): (String, String) => String = {
    val t = prepare(2, a => f(a(0), a(1)))
    (s1, s2) => t(Array(s1, s2))
  }

  private def prepare(arity: Int, f: Array[String] => String): Array[String] => String =
    prepareF[Id](arity, f)

  private def prepareF[F[_]: Functor](arity: Int, f: Array[String] => F[String]): F[Array[String] => String] = {
    val ids = Array.fill(arity)(newId())

    f(ids).map { templateWithIds =>
      if (templateWithIds eq null)
        _ => null
      else if (!ids.exists(templateWithIds.contains))
        _ => templateWithIds
      else {

        val regex = {
          val i = ids.mkString("|")
          val r = s"(?=$i)|(?<=$i)"
          r.r
        }

        def makeFragFn(frag: String): (Array[String], StringBuilder) => Unit =
          ids.indexWhere(frag == _) match {
            case -1 => (_, sb) => sb.append(frag)
            case i  => (a, sb) => sb.append(a(i))
          }

        val fragFns =
          regex
            .split(templateWithIds)
            .iterator
            .filter(_.nonEmpty)
            .map(makeFragFn)
            .toArray

        fragFns.length match {
          case 0 =>
            _ => ""

          case 1 =>
            val ff = fragFns(0)
            a => {
              val sb = new StringBuilder
              ff(a, sb)
              sb.toString()
            }

          case len =>
            a => {
              val sb = new StringBuilder
              var i = 0
              while (i < len) {
                fragFns(i)(a, sb)
                i += 1
              }
              sb.toString()
            }
        }
      }
    }
  }

  private def newId(): String =
    "\u0001" + UUID.randomUUID().toString.replace("-", " ") + "\u0002"
}

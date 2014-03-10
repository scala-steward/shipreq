package shipreq.base.util

/**
 * Reads values from some kind of external source.
 *
 * Interface to the source is provided by implicit Retriever[T] type-classes.
 *
 * External source value keys can be transformed implicitly using PropScope.
 */
object ExternalValueReader {

  case class Retriever[T](run: String => Either[Option[String], T])

  case class PropScope(run: String => String)

  val GlobalScope = PropScope(identity)

  def scopeByPrefix(prefix: String) =
    PropScope(prefix + _)

  def scopeByNS(ns: String) =
    if (ns.isEmpty) GlobalScope else scopeByPrefix(s"$ns.")

  def getEO[T](name: String)(implicit s: PropScope, r: Retriever[T]): Either[Option[String], T] =
    r.run(s.run(name))

  def get[T](name: String)(implicit s: PropScope, r: Retriever[T]): Either[String, T] =
    getEO(name).left.map(_ getOrElse defaultErrorMsg(name))

  def getO[T](name: String)(implicit s: PropScope, r: Retriever[T]): Option[T] =
    get(name).right.toOption

  def tryGet[T](name: String, moreNames: String*)(implicit s: PropScope, r: Retriever[T]): Either[String, T] = {
    val es = (name #:: moreNames.toStream).map(get(_))
    es.filter(_.isRight).headOption.getOrElse(es.head)
  }

  def need[T](name: String)(implicit s: PropScope, r: Retriever[T]): T =
    get(name) match {
      case Right(t)  => t
      case Left(msg) => throw new RuntimeException(msg)
    }

  def tryNeed[T](name: String, default: T)(implicit s: PropScope, r: Retriever[T]): T =
    getO(name) getOrElse default

  def tryUse[T](name: String)(f: T => Unit)(implicit s: PropScope, r: Retriever[T]): Unit =
    get(name).right foreach f

  def defaultErrorMsg(name: String)(implicit s: PropScope): String =
    s"Unable to retrieve external value: ${s.run(name)}"
}

package shipreq.webapp.server.lib

import net.liftweb.http.js.{JsCmd, JsCmds}
import scalaz.Monoid

/**
 * @since 30/05/2013
 */
object Types {

  // -------------------------------------------------------------------------------------------------------------------
  // String tags

  /** Marks a string as being an ISO-8601 representation of a datetime. */
  final case class ISO8601(value: String) extends AnyVal

  final case class ShareUrlToken(value: String) extends AnyVal

  // ===================================================================================================================
  // Type class instances

  implicit object JsCmdMonoid extends Monoid[JsCmd] {

    import JsCmds.{_Noop => Noop}

    override def zero = Noop
    override def append(a: JsCmd, b: => JsCmd) =
      if (a eq Noop) b
      else if (b eq Noop) a
      else a & b
  }
}

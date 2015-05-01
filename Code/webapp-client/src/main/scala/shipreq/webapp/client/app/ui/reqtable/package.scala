package shipreq.webapp.client.app.ui

import scalacss.ScalaCssReact._
import shipreq.base.util.Must
import Style.{reqtable => *}

/**
 * Requirements Table.
 * "Common Req View & Editor" in the prototype.
 *
 * An Excel-like table for reading and editing requirements.
 */
package object reqtable {

  def failedMust[A](a: A)(e: String): A = {
    // TODO Do something more with Must failure
    org.scalajs.dom.console.error(e)
    a
  }

  @inline def mustResolve[A](m: Must[A])(fallback: => A): A =
    m.fold(failedMust(fallback), identity)

  def textSeqEditor[A](name: String, splitFn: String => Stream[String]): TextSeqEditor[A] =
    new TextSeqEditor(name, splitFn, *.cellEditor(_), *.cellEditorErrMsg)
}

package shipreq.webapp.gen.phantomjs

import scala.scalajs.js
import scala.scalajs.js.|

@js.native
trait WriteParams extends js.Object {

  /** Open Mode. A string made of ‘r’, ‘w’, ‘a/+’, ‘b’ characters. */
  var mode: String = js.native

  /** An IANA, case insensitive, charset name. */
  var charset: String = js.native
}

@js.native
object FS extends js.Object {

  /** If the source file can’t be opened then it will throw a ‘Unable to open file SOURCE’ and hang execution.
    *
    * @param source The output file.
    */
  def write(source: String, content: String, params: WriteParams | String): Unit = js.native
}

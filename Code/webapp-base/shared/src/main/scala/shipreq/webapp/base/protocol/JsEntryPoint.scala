package shipreq.webapp.base.protocol

import boopickle.Pickler
import BinCodecRemoteFns._

/**
 * Describes a function exposed in client JS, that the server can invoke.
 */
final class JsEntryPoint[I,O] private[JsEntryPoint] (val name: String)(implicit PI: Pickler[I]) {
  implicit def pi = PI
}

/**
 * The contents of this object allow client and server to be in sync wrt exported JS names and parameter types.
 */
object JsEntryPoint {

  // TODO it's possible to have an entrypoint here with no impl in JsEntryPoints

  final val client = "Bnzklx"

  final val projectN = "x8927nh"
  val project = new JsEntryPoint[ProjectSPA, Unit](projectN)

}
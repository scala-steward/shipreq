package shipreq.webapp.client.protocol

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalaz.effect.IO
import shipreq.webapp.base.protocol.{JsEntryPoint => EP}

@JSExport(EP.client)
object JsEntryPoints {

  @inline private def entryPoint[I](ep: EP[I, Unit])(f: I => IO[Unit]): js.Function1[js.Any, Unit] = {
    import ep.ri
    ClientProtocol.jsonEffect[I](f)
  }

  @JSExport(EP.reactExamplesN)
  final val reactExamples = entryPoint(EP.reactExamples)(hahaa.ReactExamples.main)
}

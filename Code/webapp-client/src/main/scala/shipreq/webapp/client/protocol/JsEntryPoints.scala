package shipreq.webapp.client.protocol

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import shipreq.webapp.shared.protocol.{JsEntryPoint => EP}

@JSExport(EP.client)
object JsEntryPoints {

  @inline private def entryPoint[I, O](ep: EP[I, O])(f: I => O): js.Function1[js.Any, Unit] = {
    import ep.ri
    ClientProtocol.jsonEffect[I](f)
  }

  @JSExport(EP.reactExamplesN)
  final val reactExamples = entryPoint(EP.reactExamples)(hahaa.ReactExamples.main)
}

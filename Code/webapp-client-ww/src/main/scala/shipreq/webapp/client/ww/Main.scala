package shipreq.webapp.client.ww

import org.scalajs.dom.MessageEvent
import org.scalajs.dom.webworkers.DedicatedWorkerGlobalScope
import scalajs.js
import scalajs.js.annotation._

@JSExport("Main")
object Main {

  @JSExport
  def main(): Unit = {
    val WW = DedicatedWorkerGlobalScope.self

    WW.onmessage = (e: MessageEvent) => {
      WW.postMessage(s"Received: [${e.data}]")
    }

  }
}
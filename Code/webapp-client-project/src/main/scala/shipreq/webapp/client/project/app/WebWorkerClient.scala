package shipreq.webapp.client.project.app

import org.scalajs.dom.webworkers.Worker
import shipreq.webapp.base.lib.LoggerJs
import shipreq.webapp.client.ww.api.Protocol.Codec.{default => codec}
import shipreq.webapp.client.ww.api._

object WebWorkerClient {

  type Instance = Client[WebWorkerCmd, codec.Reader, codec.Encoded]

  def apply(wwJsUrl: String, logger: LoggerJs): Instance = {
    lazy val worker = new Worker(wwJsUrl)
    Client.default[WebWorkerCmd](worker, logger)
  }
}

package shipreq.webapp.client.project.app

import japgolly.scalajs.react._
import org.scalajs.dom.webworkers.Worker
import shipreq.webapp.base.AssetManifest
import shipreq.webapp.client.ww.api._
import shipreq.webapp.client.ww.api.Protocol.Codec.{default => codec}

trait WebWorkerClient extends Client[WebWorkerCmd, codec.Reader]

object WebWorkerClient {

  val Instance: WebWorkerClient = {
    lazy val worker = new Worker(AssetManifest.webappClientWwJs)
    lazy val client = Client.default[WebWorkerCmd](worker)
    new WebWorkerClient {
      override def post[A](cmd: WebWorkerCmd[A])(implicit readResult: codec.Reader[A]) =
        client.post(cmd)
    }
  }

  implicit def reusability: Reusability[WebWorkerClient] =
    Reusability.byRef
}

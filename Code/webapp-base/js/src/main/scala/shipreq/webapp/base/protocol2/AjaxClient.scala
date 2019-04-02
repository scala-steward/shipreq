package shipreq.webapp.base.protocol2

import boopickle._
import japgolly.scalajs.react.AsyncCallback
import japgolly.scalajs.react.extra.Ajax
import org.scalajs.dom.ext.AjaxException

trait AjaxClient[F[_]] {
  def apply[Req, Res](p: Protocol.Ajax[F, Req, Res])
                     (req: p.protocol.request.Type): AsyncCallback[p.protocol.response.Type]
}

object AjaxClient {

  object Binary extends AjaxClient[Pickler] {
    override def apply[Req, Res](p: Protocol.Ajax[Pickler, Req, Res])
                                (req: p.protocol.request.Type): AsyncCallback[p.protocol.response.Type] = {

      val reqBinary = BinaryJs.encode(req)(p.protocol.request.codec)

      Ajax("POST", p.url.relativeUrl)
        .setRequestHeader("Content-Type", "application/octet-stream")
        .and(_.responseType = "arraybuffer")
        .send(reqBinary)
        .asAsyncCallback
        .map { xhr =>
          if (xhr.status == 200)
            BinaryJs.decodeUnsafe(xhr.response)(p.protocol.response.codec)
          else
            throw AjaxException(xhr)
        }
    }
  }

}
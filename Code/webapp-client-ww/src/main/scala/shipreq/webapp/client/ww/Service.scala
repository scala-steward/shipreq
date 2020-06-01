package shipreq.webapp.client.ww

import japgolly.scalajs.react.AsyncCallback
import shipreq.webapp.client.ww.api.WebWorkerCmd

object Service extends Server.Service[WebWorkerCmd] {
  import WebWorkerCmd._

  val state = new WebWorkerState

  override def apply[A](cmd: WebWorkerCmd[A]): AsyncCallback[A] =
    cmd match {

      case SetProject(p) =>
        state.setProject(p).asAsyncCallback.ret(NoResult)

      case UpdateProject(ves) =>
        state.updateProject(ves).asAsyncCallback.ret(NoResult)

      case GraphUseCaseStepFlow(a, b, c) =>
        Graphs.useCaseStepFlow(a, b, c).toSvg

      case GraphReqImplications(a, b, c, d, e) =>
        Graphs.implicationFocused(a, b, c, d, e).toSvg

      case a: GraphAllImplications =>
        Graphs.implicationAll(a).toSvg
    }
}

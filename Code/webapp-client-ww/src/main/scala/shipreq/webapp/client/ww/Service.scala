package shipreq.webapp.client.ww

import japgolly.scalajs.react.AsyncCallback
import shipreq.webapp.client.ww.GraphViz.DOT
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

      case GraphUseCaseFlow(ord, id, ctx) =>
        for {
          _ <- state.await(ord)
          p <- state.acProject
          x <- new UseCaseFlowGraph(id, p, ctx).svg
        } yield x

      case GraphReqImplications(ord, focus, filterDead) =>
        for {
          _ <- state.await(ord)
          p <- state.acProject
          x <- new ReqImpGraph(focus, filterDead, p).svg
        } yield x

      case GraphAllImplications(ord, filterDead, scope, config) =>
        for {
          _  <- state.await(ord)
          p  <- state.acProject
          pt <- state.acPlainText
          x  <- new ProjectImpGraph(p, pt, filterDead, scope, config).svg
        } yield x

      case GraphInline(dot) =>
        DOT(dot).toSvg
    }
}

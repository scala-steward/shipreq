package shipreq.webapp.server.app

import com.google.cloud.trace.core._
import java.nio.ByteBuffer
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.http.{LiftResponse, Req}
import scalaz.{-\/, \/-}
import shipreq.base.ops.StackdriverTrace
import shipreq.base.util.FxModule._
import shipreq.base.util.Url
import shipreq.webapp.server.logic.{Server, Trace}

object TraceInterpreter {

  type ForLift[F[_]] = Trace.Algebra[F, Req, Box[LiftResponse]]

  def apply(traceConfig: StackdriverTrace.Cfg): ForLift[Fx] =
    new ForLift[Fx] {

      val tracer = traceConfig.getTracer()

      val component = "webapp"

      private val getSpanContext: Fx[SpanContext] =
        Fx(com.google.cloud.trace.Trace.getSpanContextHandler.current)

      override def generic[A](name: String)(f: => Fx[A]) =
        for {
          span <- Fx(tracer.startSpan(name))
          res  <- f
        } yield {
          tracer.endSpan(span)
          res
        }

      override def sub[A](name: String)(f: => Fx[A]) =
        getSpanContext.flatMap(ctx =>
          if (ctx.getTraceId == TraceId.invalid)
            f // Don't trace
          else
            generic(name)(f))

      val str200 = "200"

      override def http(req: Req, path: Url.Relative)(f: => Fx[Box[LiftResponse]]) = {
        val uri = path.relativeUrl
        for {
          ctx <- Fx(tracer.startSpan(uri))
          res <- f
        } yield {

          val labels = Labels.builder()
            .add(StackdriverTrace.Label.Component, component)
            .add(StackdriverTrace.Label.HttpUrl, uri)
            .add(StackdriverTrace.Label.HttpMethod, req.requestType.method)

          req.userAgent.foreach(
            labels.add(StackdriverTrace.Label.HttpUserAgent, _))

          res match {
            case Full(lr) =>
              val r = lr.toResponse
              labels.add(StackdriverTrace.Label.HttpStatusCode, if (r.code == 200) str200 else r.code.toString)
              labels.add(StackdriverTrace.Label.HttpResponseSize, r.size.toString)
            case x: Failure =>
              x.rootExceptionCause.foreach(setError(ctx, labels)(_, 500))
            case Empty => ()
          }

          tracer.annotateSpan(ctx, labels.build())
          tracer.endSpan(ctx)
          res
        }
      }

      override def serverSideProc(name: String, input: ByteBuffer)(f: => Server.SspResponse[Fx]) =
        for {
          ctx <- Fx(tracer.startSpan(name))
          res <- f
        } yield {

          val labels = Labels.builder()
            .add(StackdriverTrace.Label.Component, component)
            .add(StackdriverTrace.Label.HttpMethod, "POST")
            .add(StackdriverTrace.Label.HttpRequestSize, input.limit.toString)

          res match {
            case \/-(output) =>
              labels.add(StackdriverTrace.Label.HttpStatusCode, str200)
              labels.add(StackdriverTrace.Label.HttpResponseSize, output.limit.toString)
            case -\/(e) =>
              val (t: Throwable, status: Int) =
                e match {
                  case Server.RequestPickleError (x) => (x, 400)
                  case Server.ResponsePickleError(x) => (x, 500)
                }
              setError(ctx, labels)(t, status)
          }

          tracer.annotateSpan(ctx, labels.build())
          tracer.endSpan(ctx)
          res
        }

      private def setError(ctx: TraceContext, labels: Labels.Builder)(t: Throwable, status: Int): Unit = {
        labels.add(StackdriverTrace.Label.ErrorMessage, t.getMessage)
        labels.add(StackdriverTrace.Label.HttpStatusCode, status.toString)
        tracer.setStackTrace(ctx, ThrowableStackTraceHelper.createBuilder(t).build)
      }

    }
}

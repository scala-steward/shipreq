package shipreq.base.ops

import japgolly.microlibs.stdlib_ext.StdlibExt._
import java.time.Instant
import kamon.Kamon
import shipreq.base.util.FxModule._

object TraceWithKamon {

  val algebraFx: Trace.Algebra[Fx] =
    new Trace.Algebra[Fx] {
      override type Span = kamon.trace.Span
      private[this] val Span = kamon.trace.Span
      private[this] val SpanContextKey = Span.ContextKey

      private def withNewSpan[A](createSpan: => Span)(f: Span => Fx[A]): Fx[A] =
        Fx {
          val span = createSpan
          unsafeWithActiveSpan(span)(f(span).unsafeRun())
        }

      private def withActiveSpan[A](span: Span, f: Fx[A]): Fx[A] =
        Fx(unsafeWithActiveSpan(span)(f.unsafeRun()))

      private def unsafeWithActiveSpan[A](span: Span)(body: => A): A =
        try
          Kamon.withContextKey(SpanContextKey, span) {
            body
          }
        catch {
          case t: Throwable =>
            setError(span, t)
            throw t
        }
        finally
          span.finish()

      private def setError(span: Span, err: Throwable): Unit = {
        span.tag("error", true)
        span.tag("event", "error")
        span.tag("error.kind", err.getClass.getName)
        span.tag("message", err.getMessage)
        span.tag("stack", err.stackTraceAsString)
      }

      override def newSpan[A](name: String)(f: Span => Fx[A]): Fx[A] =
        withNewSpan(Kamon.buildSpan(name).start())(f)

      override def newSubSpan[A](name: String, parent: Span)(f: Span => Fx[A]): Fx[A] =
        withNewSpan(Kamon.buildSpan(name).asChildOf(parent).start())(f)

      override def _propagateCtx[A]: Fx[Fx[A] => Fx[A]] =
        Fx((f: Fx[A]) => {
          val span = Kamon.currentSpan()
          if (span eq null)
            f
          else
            withActiveSpan(span, f)
        })

      private[this] val attrInterpretter = Trace.Attr.interpret[Span, Unit](
          endpointName     = (s, a) => s.tag("endpoint.name", a.value),
          httpMethod       = (s, a) => s.tag("http.method", a.value),
          httpRemoteHost   = (s, a) => s.tag("http.remote_host", a.value),
          httpRemotePort   = (s, a) => s.tag("http.remote_port", a.value),
          httpRequestSize  = (s, a) => s.tag("http.request_size", a.value),
          httpResponseSize = (s, a) => s.tag("http.response_size", a.value),
          httpSessionId    = (s, a) => s.tag("http.session_id", a.value),
          httpStatusCode   = (s, a) => s.tag("http.status_code", a.value),
          httpUri          = (s, a) => s.tag("http.uri", a.value),
          httpUrl          = (s, a) => s.tag("http.url", a.value),
          httpUserAgent    = (s, a) => s.tag("http.user_agent", a.value),
          shipReqProjectId = (s, a) => s.tag("shipreq.project_id", a.value),
          shipReqUserId    = (s, a) => s.tag("shipreq.user_id", a.value),
          error            = (s, a) => setError(s, a.value))

      override def addAttrs(attrs: List[Trace.Attr])(implicit span: Span): Fx[Unit] =
        Fx(attrs.foreach(attrInterpretter(span, _)))

      override def sqlTracer(spanName: String) =
        Some(new SqlTracer {
          override def logExecute(method: String, sql: String, batches: Int,
                                  err: Option[Throwable], startTimeNs: Long, endTimeNs: Long): Unit = {
            val from = Instant.ofEpochMilli(startTimeNs / 1000000)
            val end = from.plusNanos(endTimeNs - startTimeNs)
            val span = Kamon.buildSpan(spanName).withFrom(from).start()
            span.tag("jdbc.class", "PreparedStatement")
            span.tag("jdbc.method", method)
            span.tag("jdbc.sql", sql)
            span.tag("jdbc.batches", batches: Long)
            err.foreach(setError(span, _))
            span.finish(end)
          }
        })

    }

}

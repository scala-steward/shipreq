package shipreq.base.ops

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.trace.{Trace => T, Tracer}
import com.google.cloud.trace.core.{ConstantTraceOptionsFactory, Labels, RateLimitingTraceOptionsFactory, ThrowableStackTraceHelper}
import com.google.cloud.trace.service.TraceGrpcApiService
import japgolly.microlibs.config.ConfigParser.Implicits.Defaults._
import japgolly.microlibs.config._
import java.io.FileInputStream
import scalaz.syntax.applicative._
import shipreq.base.util.Memo

object TraceWithStackdriver {

  final case class Cfg(

    /** GCP project the traces should be associated with. */
    projectId: String,

    /** Credentials file to be used for Stackdriver Trace API calls. */
    credentials: Option[String],

    /** The maximum number of seconds a Trace will be buffered locally before
      * being written to the Stackdriver Trace API. */
    scheduledDelaySec: Int,

    /** The maximum local buffer size (in bytes) to use before flushing to the
      * Stackdriver Trace API. */
    bufferSizeBytes: Int,

    /** Ensures that on average no more than n traces are issued during any given second, with sustained requests
      * being smoothly spread over each second. */
    limitTracesPerSec: Double) {

    private def traceService = {
      val b = TraceGrpcApiService.builder()

      b.setProjectId(projectId)

      for (filename <- credentials) {
        val fin = new FileInputStream(filename)
        val cred = GoogleCredentials.fromStream(fin)
        fin.close()
        b.setCredentials(cred)
      }

      b.setScheduledDelay(scheduledDelaySec)

      b.setBufferSize(bufferSizeBytes)

      val stackTraceEnabled = true
      b.setTraceOptionsFactory(
        limitTracesPerSec match {
        case a if a > 0 => new RateLimitingTraceOptionsFactory(a, stackTraceEnabled)
        case _          => new ConstantTraceOptionsFactory(true, stackTraceEnabled)
      })

      b.build()
    }

    val init: () => Unit =
      Memo.fn0(T.init(traceService))

    def getTracer(): Tracer = {
      init()
      T.getTracer()
    }

    def sqlTracer() =
      TraceWithStackdriver.sqlTracer(getTracer())
  }

  def config: Config[Option[Cfg]] =
    (     Config.get     [String]("projectId")
      |@| Config.get     [String]("credentials")
      |@| Config.getOrUse[Int]   ("scheduledDelaySec", 15).ensure(_ >= 0, "Must be ≥ 0")
      |@| Config.getOrUse[Int]   ("bufferSizeBytes", 32 * 1024).ensure(_ >= 0, "Must be ≥ 0")
      |@| Config.getOrUse[Double]("limitTracesPerSec", 100)
      ) ((projectId, credentials, scheduledDelaySec, bufferSizeBytes, limitTracesPerSec) =>
      projectId.map(id =>
        Cfg(
          projectId         = id,
          credentials       = credentials,
          scheduledDelaySec = scheduledDelaySec,
          bufferSizeBytes   = bufferSizeBytes,
          limitTracesPerSec = limitTracesPerSec)))

  private object Label {
    // https://cloud.google.com/trace/docs/reference/v1/rpc/google.devtools.cloudtrace.v1
    final val Agent              = "/agent"
    final val Component          = "/component"
    final val ErrorMessage       = "/error/message"
    final val ErrorName          = "/error/name"
    final val HttpClientCity     = "/http/client_city"
    final val HttpClientCountry  = "/http/client_country"
    final val HttpClientProtocol = "/http/client_protocol"
    final val HttpClientRegion   = "/http/client_region"
    final val HttpHost           = "/http/host"
    final val HttpMethod         = "/http/method"
    final val HttpRedirectedUrl  = "/http/redirected_url"
    final val HttpRequestSize    = "/http/request/size"
    final val HttpResponseSize   = "/http/response/size"
    final val HttpStatusCode     = "/http/status_code"
    final val HttpUrl            = "/http/url"
    final val HttpUserAgent      = "/http/user_agent"
    final val Pid                = "/pid"
    final val Stacktrace         = "/stacktrace"
    final val Tid                = "/tid"

    // Custom
    final val EndpointName       = "/endpoint/name"
    final val HttpRemoteHost     = "/http/remote/host"
    final val HttpRemotePort     = "/http/remote/port"
    final val HttpSessionId      = "/http/session_id"
    final val HttpUri            = "/http/uri"
    final val ShipReqProjectId   = "/shipreq/project_id"
    final val ShipReqUserId      = "/shipreq/user_id"
  }

//  def stackdriverAlgebra(cfg: StackdriverTrace.Cfg): Trace.Algebra[Fx] =
//    new Trace.Algebra[Fx] {
//      import com.google.cloud.trace.core._
//
//      private[this] val tracer = cfg.getTracer()
//
//      override type Span = TraceContext
//
//      override def newSpan[A](name: String)(f: Span => Fx[A]): Fx[A] =
//        for {
//          span <- Fx(tracer.startSpan(name))
//          res <- f(span)
//        } yield {
//          tracer.endSpan(span) -- handle exceptions
//          res
//        }
//
////      private val getSpanContext: Fx[SpanContext] =
////        Fx(com.google.cloud.trace.Trace.getSpanContextHandler.current)
////
////      override def sub[A](name: String)(f: => Fx[A]) =
////        getSpanContext.flatMap(ctx =>
////          if (ctx.getTraceId == TraceId.invalid)
////            f // Don't trace
////          else
////            generic(name)(f))
//
//      override def newSubSpan[A](name: String, parent: Span)(f: Span => Fx[A]): Fx[A] =
//        how to use parent span
//        newSpan(name)(f)
//
//      private[this] val attrInterpretter = Trace.Attr.interpret[Labels.Builder, Throwable](
//          endpointName     = (l, a) => {l.add(StackdriverTrace.Label.EndpointName, a.value); null},
//          error            = (l, a) => {l.add(StackdriverTrace.Label.ErrorMessage, a.value.getMessage); a.value},
//          httpMethod       = (l, a) => {l.add(StackdriverTrace.Label.HttpMethod, a.value); null},
//          httpRemoteHost   = (l, a) => {l.add(StackdriverTrace.Label.HttpRemoteHost, a.value); null},
//          httpRemotePort   = (l, a) => {l.add(StackdriverTrace.Label.HttpRemotePort, a.value.toString); null},
//          httpRequestSize  = (l, a) => {l.add(StackdriverTrace.Label.HttpRequestSize, a.value.toString); null},
//          httpResponseSize = (l, a) => {l.add(StackdriverTrace.Label.HttpResponseSize, a.value.toString); null},
//          httpSessionId    = (l, a) => {l.add(StackdriverTrace.Label.HttpSessionId, a.value); null},
//          httpStatusCode   = (l, a) => {l.add(StackdriverTrace.Label.HttpStatusCode, a.str); null},
//          httpUri          = (l, a) => {l.add(StackdriverTrace.Label.HttpUri, a.value); null},
//          httpUrl          = (l, a) => {l.add(StackdriverTrace.Label.HttpUrl, a.value); null},
//          httpUserAgent    = (l, a) => {l.add(StackdriverTrace.Label.HttpUserAgent, a.value); null},
//          shipReqProjectId = (l, a) => {l.add(StackdriverTrace.Label.ShipReqProjectId, a.value.toString); null},
//          shipReqUserId    = (l, a) => {l.add(StackdriverTrace.Label.ShipReqUserId, a.value.toString); null})
//
//      override def addAttrs(attrs: List[Trace.Attr])(implicit span: Span): Fx[Unit] =
//        Fx {
//          val labels = Labels.builder()
//          for (a <- attrs) {
//            val t = attrInterpretter(labels, a)
//            if (t ne null)
//              tracer.setStackTrace(span, ThrowableStackTraceHelper.createBuilder(t).build)
//          }
//          tracer.annotateSpan(span, labels.build())
//        }
//    }

  def sqlTracer(tracer: Tracer): SqlTracer =
    new SqlTracer {
      override def executePreparedStatement[@specialized(Boolean, Int, Long) A](method : String,
                                                                                sql    : String,
                                                                                batches: Int,
                                                                                run    : () => A): A = {
        val ctx = tracer.startSpan("JDBC")
        val labels = Labels.builder()
          .add("/jdbc/class", "PreparedStatement")
          .add("/jdbc/method", method)
          .add("/jdbc/sql", sql)
          .add("/jdbc/batches", batches.toString)

        try {
          val a = run()
          tracer.annotateSpan(ctx, labels.build())
          tracer.endSpan(ctx)
          a

        } catch {
          case t: Throwable =>
            labels.add(Label.ErrorMessage, t.getMessage)
            tracer.setStackTrace(ctx, ThrowableStackTraceHelper.createBuilder(t).build)
            tracer.annotateSpan(ctx, labels.build())
            tracer.endSpan(ctx)
            throw t
        }
      }
    }
}

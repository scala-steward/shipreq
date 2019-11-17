package shipreq.webapp.server.app

import io.prometheus.client.exporter.MetricsServlet
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/** Version of Prometheus' [[MetricsServlet]] that
  *
  * 1. exposes it's service method
  * 2. (optionally) requires a bearer token
  */
final class PrometheusMetricsServlet(expectedBearerToken: Option[String]) extends MetricsServlet {

  private[this] val serviceFn: (HttpServletRequest, HttpServletResponse) => Unit =
    expectedBearerToken match {
      case None =>
        super.service(_, _)

      case Some(token) =>
        val expectedAuth = s"Bearer $token"
        (req, resp) => {
          val auth = req.getHeader("Authorization")
          if (auth eq null)
            // Pretend it doesn't exist if someone hasn't already specified an auth token
            resp.setStatus(404)
          else if (expectedAuth != auth)
            resp.setStatus(401)
          else
            super.service(req, resp)
        }
    }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit =
    serviceFn(req, resp)
}

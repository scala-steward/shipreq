package shipreq.webapp.server.http

import com.typesafe.scalalogging.StrictLogging
import javax.servlet.http.HttpServletResponse
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse}
import org.eclipse.jetty.servlets.CrossOriginFilter
import scala.jdk.CollectionConverters._
import shipreq.webapp.server.config.Global

final class CrossOriginServletFilter extends CrossOriginFilter with StrictLogging {
  override def init(orig: FilterConfig): Unit = {

    val additions = {
      import CrossOriginFilter._
      val domain = Global.config.server.baseUrl.value

      logger.info(s"${orig.getFilterName} CORS restricting origin to $domain")

      Map(
        ALLOWED_ORIGINS_PARAM              -> domain,
        ACCESS_CONTROL_ALLOW_ORIGIN_HEADER -> domain,
      )
    }

    val origKeys = orig.getInitParameterNames.asScala.toSet
    val allKeys  = origKeys ++ additions.keySet

    val newConfig: FilterConfig =
      new FilterConfig {

        override def getFilterName =
          orig.getFilterName

        override def getServletContext =
          orig.getServletContext

        override def getInitParameterNames =
          allKeys.iterator.asJavaEnumeration

        override def getInitParameter(name: String) =
          additions.getOrElse(name, orig.getInitParameter(name))
      }

    super.init(newConfig)
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    response match {
      case r : HttpServletResponse =>
        // This is required because underlying CrossOriginFilter does add Origin to Vary.
        // Origin is required because Chrome doesn't save the origin when it caches, so when it reads back from disk
        // it sees that the cached value doesn't pass the origin rules, and then it sees Origin isn't in the vary list
        // and so it gives up without trying again *with* the origin set properly.
        //
        // https://stackoverflow.com/questions/44800431/caching-effect-on-cors-no-access-control-allow-origin-header-is-present-on-th
        r.addHeader("Vary", "Origin, Accept-Encoding")
    }
    super.doFilter(request, response, chain)
  }
}

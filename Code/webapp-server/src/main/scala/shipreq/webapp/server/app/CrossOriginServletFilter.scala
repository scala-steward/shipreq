package shipreq.webapp.server.app

import com.typesafe.scalalogging.StrictLogging
import javax.servlet.FilterConfig
import org.eclipse.jetty.servlets.CrossOriginFilter
import scala.jdk.CollectionConverters._

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
}

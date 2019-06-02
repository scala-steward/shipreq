package shipreq.webapp.ssr

import scala.xml.XML
import shipreq.base.util.{Permission, Url}
import shipreq.webapp.base.user.Username

trait SsrAlgebra[F[_]] {
  import SsrAlgebra.Html
  import SsrSharedData._

  def warmup: F[Unit]

  def public(publicRegistration: Permission): F[(Url.Relative, Option[Username]) => F[Option[Html]]]

  def projectSpaLoader: F[ProjectSpaLoaderData => F[Option[Html]]]
}

object SsrAlgebra {

  final case class Html(value: String) {
    val xml = XML.loadString(value)
  }

}

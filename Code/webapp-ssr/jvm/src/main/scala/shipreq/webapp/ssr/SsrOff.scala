package shipreq.webapp.ssr

import cats.Applicative
import shipreq.base.util.Permission
import shipreq.webapp.ssr.SsrAlgebra.Html

final case class SsrOff[F[_]]()(implicit F: Applicative[F]) extends SsrAlgebra[F] {
  private[this] val none             = F.pure(Option.empty[Html])
  override def warmup                = F.unit
  override def public(p: Permission) = F.pure((_, _) => none)
  override def projectSpaLoader      = F.pure(_ => none)
}

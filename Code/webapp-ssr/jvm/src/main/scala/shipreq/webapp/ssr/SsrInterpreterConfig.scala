package shipreq.webapp.ssr

import japgolly.clearconfig._
import scalaz.Applicative

final case class SsrInterpreterConfig() { // TODO

  def instance[F[_]: Applicative](prometheus: Boolean): SsrAlgebra[F] =
    new SsrMinimal[F]
}

object SsrInterpreterConfig {

  def defn: ConfigDef[SsrInterpreterConfig] =
    ConfigDef.const(apply())
}
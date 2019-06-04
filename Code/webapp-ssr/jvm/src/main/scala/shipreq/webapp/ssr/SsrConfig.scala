package shipreq.webapp.ssr

import japgolly.clearconfig._
import scalaz.Applicative

final case class SsrConfig(enabled: Boolean) {

  def instance[F[_]: Applicative]: SsrAlgebra[F] =
    if (enabled)
      new SsrMinimal[F]
    else
      new SsrOff[F]
}

object SsrConfig {

  def config: ConfigDef[SsrConfig] =
    ConfigDef.getOrUse("enabled", true).map(apply)
}
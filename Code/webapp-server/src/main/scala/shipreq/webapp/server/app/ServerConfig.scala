package shipreq.webapp.server.app

import japgolly.clearconfig._
import monocle.macros.Lenses
import scalaz.syntax.applicative._
import shipreq.base.db.DbConfig
import shipreq.webapp.server.ServerLogicConfig

@Lenses
final case class ServerConfig(db    : DbConfig,
                              server: ServerLogicConfig,
                              report: ConfigReport)

object ServerConfig {

  def config[A](cfgA: ConfigDef[A]): ConfigDef[(ServerConfig, A)] =
    ( DbConfig.config |@|
      ServerLogicConfig.config |@|
      cfgA
    ).tupled
      .withReport
      .map { case ((db, svr, a), report) =>
        val cfg = ServerConfig(db, svr, report)
        (cfg, a)
      }

}
package shipreq.webapp.server.app

import japgolly.clearconfig._
import monocle.macros.Lenses
import scalaz.syntax.applicative._
import shipreq.base.db.DbConfig
import shipreq.webapp.server.ServerLogicConfig

@Lenses
final case class ServerConfig(db    : DbConfig,
                              server: ServerLogicConfig)

object ServerConfig {

  def config: ConfigDef[ServerConfig] =
    ( DbConfig.config |@|
      ServerLogicConfig.config
    )(apply)

}
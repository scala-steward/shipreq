package shipreq.webapp.server.logic

import scalaz.{Monad, ~>}
import shipreq.taskman.api.TaskmanApi
import shipreq.webapp.server.ServerConfig

/**
  * All server logic.
  */
final case class ServerLogic[F[_]](publicSpa    : PublicSpaLogic[F],
                                   homeSpa      : HomeSpaLogic  [F],
                                   projectServer: ProjectServer [F])

object ServerLogic {

  def create[D[_] : Monad : DB.Algebra,
             F[_] : Monad : ProjectServer.StoreAlgebra : Server.Algebra : TaskmanApi]
            (b: ProjectServer.BroadcastTo)
            (implicit runDB: D ~> F, config: ServerConfig)
            : ServerLogic[F] =
    ServerLogic(
      PublicSpaLogic[D, F](config.allowRegister),
      HomeSpaLogic[D, F],
      ProjectServer[D, F](b))
}
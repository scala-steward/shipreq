package shipreq.webapp.server.logic

import scalaz.{Monad, ~>}
import shipreq.base.util.Permission
import shipreq.taskman.api.TaskmanApi

/**
  * All server logic.
  */
final case class ServerLogic[F[_]](publicSpa    : PublicSpaLogic[F],
                                   homeSpa      : HomeSpaLogic  [F],
                                   projectServer: ProjectServer [F])

object ServerLogic {

  def create[D[_] : Monad : DB.Algebra,
             F[_] : Monad : ProjectServer.StoreAlgebra : Server.Algebra : TaskmanApi]
            (publicRegistration: Permission, b: ProjectServer.BroadcastTo)
            (implicit runDB: D ~> F)
            : ServerLogic[F] =
    ServerLogic(
      PublicSpaLogic[D, F](publicRegistration),
      HomeSpaLogic[D, F],
      ProjectServer[D, F](b))
}
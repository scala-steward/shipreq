package shipreq.webapp.server.logic

import scalaz.{Monad, ~>}
import shipreq.webapp.base.protocol.PublicSpaProtocols

trait PublicSpaLogic[F[_]] {
  val initData: F[PublicSpaProtocols.InitData]
}

object PublicSpaLogic {

  def apply[D[_], F[_]](implicit db: DB.ForPublicSpa[D],
                        runDB: D ~> F,
                        svr: Server.Algebra[F],
                        D: Monad[D],
                        F: Monad[F]): PublicSpaLogic[F] =
    new PublicSpaLogic[F] {

      val initData: F[PublicSpaProtocols.InitData] =
        F point ()

    }
}

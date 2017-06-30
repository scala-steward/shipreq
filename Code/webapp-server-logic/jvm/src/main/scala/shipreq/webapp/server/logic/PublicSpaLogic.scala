package shipreq.webapp.server.logic

import scalaz.{-\/, Monad, \/, \/-, ~>}
import scalaz.syntax.monad._
import shipreq.webapp.base.protocol.ErrorMsg
import shipreq.webapp.base.user._
import shipreq.webapp.base.validation._
import shipreq.webapp.base.validation.Implicits._
import shipreq.webapp.client.public.protocol._
import shipreq.webapp.client.public.protocol.PublicSpaProtocols._
import shipreq.taskman.api.{Msg, TaskmanApi}
import WebappTaskmanConverters._
import Implicits._

trait PublicSpaLogic[F[_]] {
  val initData: F[InitData]
}

object PublicSpaLogic {

  def apply[D[_], F[_]](implicit db     : DB.ForPublicSpa[D],
                                 runDB  : D ~> F,
                                 svr    : Server.Algebra[F],
                                 taskman: TaskmanApi[F],
                                 D      : Monad[D],
                                 F      : Monad[F]): PublicSpaLogic[F] =
    new PublicSpaLogic[F] {

      val landingPageFn: F[LandingPageProtocol.Fn.Instance] =
        svr.createServerSideProc(LandingPageProtocol.Fn)(i =>
          LandingPageProtocol.Request.validator(i.toValidatorInput).onValid { req =>
            val msg = Msg.LandingPageHit(
              name       = req.name.value,
              email      = req.email.toTaskman,
              msg        = req.msg,
              newsletter = req.newsletter)
            taskman.submitMsg(msg).void
          }
        )

      val initData: F[InitData] =
        F.map(landingPageFn)(InitData)

    }
}

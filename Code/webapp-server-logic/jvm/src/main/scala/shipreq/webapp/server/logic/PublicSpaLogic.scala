package shipreq.webapp.server.logic

import scalaz.{-\/, Monad, \/, \/-, ~>}
import scalaz.syntax.monad._
import shipreq.base.util._
import shipreq.webapp.base.protocol.ErrorMsg
import shipreq.webapp.base.user._
import shipreq.webapp.base.validation._
import shipreq.webapp.base.validation.Implicits._
import shipreq.webapp.client.public._
import shipreq.webapp.client.public.PublicSpaProtocols._
import shipreq.taskman.api.{Msg, TaskmanApi}
import WebappTaskmanConverters._
import Implicits._

trait PublicSpaLogic[F[_]] {
  val initData: F[InitData]
}

object PublicSpaLogic {

  def apply[D[_], F[_]](allowRegister   : Permission)
                       (implicit db     : DB.ForPublicSpa[D],
                                 runDB  : D ~> F,
                                 svr    : Server.Algebra[F],
                                 taskman: TaskmanApi[F],
                                 D      : Monad[D],
                                 F      : Monad[F]): PublicSpaLogic[F] = {

    val landingPageFn: F[LandingPage.Fn.Instance] =
      svr.createServerSideProc(LandingPage.Fn)(i =>
        LandingPage.Request.validator(i.toValidatorInput).onValid { req =>
          val msg = Msg.LandingPageHit(
            name       = req.name.value,
            email      = req.email.toTaskman,
            msg        = req.msg,
            newsletter = req.newsletter)
          taskman.submitMsg(msg).void
        }
      )

    val registerFn1: F[Register.Fn1.Instance] =
      svr.createServerSideProc(Register.Fn1)(i =>
        ???
      )

    val registerFn2: F[Register.Fn2.Instance] =
      svr.createServerSideProc(Register.Fn2)(i =>
        ???
      )

    val loginFn: F[Login.Fn.Instance] =
      svr.createServerSideProc(Login.Fn)(i =>
        ???
      )

    val resetPasswordFn1: F[ResetPassword.Fn1.Instance] =
      svr.createServerSideProc(ResetPassword.Fn1)(i =>
        ???
      )

    val resetPasswordFn2: F[ResetPassword.Fn2.Instance] =
      svr.createServerSideProc(ResetPassword.Fn2)(i =>
        ???
      )

    new PublicSpaLogic[F] {
      val initData: F[InitData] =
        for {
          a <- landingPageFn
          b <- registerFn1
          c <- registerFn2
          d <- loginFn
          e <- resetPasswordFn1
          f <- resetPasswordFn2
        } yield InitData(a, allowRegister, b, c, d, e, f)
    }
  }
}

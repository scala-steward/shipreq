package shipreq.webapp.server.logic

import java.time.{Duration, Instant}
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
import shipreq.webapp.base.PublicUrls
import shipreq.webapp.base.data.SecurityToken
import shipreq.webapp.server.ServerConfig

trait PublicSpaLogic[F[_]] {
  val initData: F[InitData]
}

object PublicSpaLogic {

  def apply[D[_], F[_]](implicit config  : ServerConfig,
                                 db      : DB.ForPublicSpa[D],
                                 runDB   : D ~> F,
                                 security: Security.Algebra[F],
                                 svr     : Server.Algebra[F],
                                 taskman : TaskmanApi[F],
                                 D       : Monad[D],
                                 F       : Monad[F]): PublicSpaLogic[F] = {

    val absUrlRegister2 = config.baseUrl / PublicUrls.register2
    val absUrlResetPwd2 = config.baseUrl / PublicUrls.resetPassword2

    def isExpired_?(startTime: Instant, timeToLive: Duration, now: Instant): Boolean =
      startTime plus timeToLive isBefore now

    def isConfirmationTokenExpired(dateIssued: Instant, now: Instant): Boolean =
      isExpired_?(dateIssued, config.confirmationTokenLifespan, now)

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    val landingPageFn: F[LandingPage.Fn.Instance] =
      svr.createServerSideProc(LandingPage.Fn)(
        _.validate.onValid { req =>
          val msg = Msg.LandingPageHit(
            name       = req.name.value,
            email      = req.email.toTaskman,
            msg        = req.msg,
            newsletter = req.newsletter)
          taskman.submitMsg(msg).void
        }
      )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    val registerFn1: F[Register.Fn1.Instance] = {
      import DB.UserRegistration

      def registerInDb(emailAddr: EmailAddr, now: Instant): D[Msg] =
        db.inDbTransaction(
          db.getUserRegistration(emailAddr).flatMap {
            case None    => onNewUser(emailAddr)
            case Some(u) => preRegistrationMsg(emailAddr, u, now)
          })

      def onNewUser(email: EmailAddr): D[Msg] =
        db.createUserPlaceholder(email).map(registrationRequestedTask(email, _))

      def preRegistrationMsg(email: EmailAddr, u: UserRegistration, now: Instant): D[Msg] =
        u match {
          case _: UserRegistration.Complete =>
            D pure onAlreadyRegistered(email)
          case r: UserRegistration.Pending =>
            if (isConfirmationTokenExpired(r.tokenSentAt, now))
              onTokenExpired(email, r.id)
            else
              D pure onTokenReusable(email, r.token)
        }

      def onTokenReusable(email: EmailAddr, token: SecurityToken): Msg =
        registrationRequestedTask(email, token)

      def onTokenExpired(email: EmailAddr, id: UserId): D[Msg] =
        db.updateUserRegistrationToken(id).map(registrationRequestedTask(email, _))

      def onAlreadyRegistered(email: EmailAddr): Msg =
        Msg.ReRegistrationAttempted(email.toTaskman)

      def registrationRequestedTask(email: EmailAddr, token: SecurityToken): Msg =
        Msg.RegistrationRequested(email.toTaskman, absUrlRegister2(token).absoluteUrl)

      svr.createServerSideProc(Register.Fn1)(
        security.protectFn(
          config.allowRegister match {

            case Allow => i =>
              UserValidators.emailAddr.unnamed(i.value).onValid(emailAddr =>
                for {
                  now <- svr.now
                  msg <- runDB(registerInDb(emailAddr, now))
                  _   <- taskman.submitMsg(msg)
                } yield ()
              )

            case Deny =>
              _ => F.pure(-\/(ErrorMsg("Registration is disabled.")))
          }
        )
      )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    val registerFn2: F[Register.Fn2.Instance] =
      svr.createServerSideProc(Register.Fn2)(i =>
//-  def render = {
//-    securityProvider().enforceHumanSpeed()
//-    validateToken_!()
//-    form.csssel(vars, vars = _) & ":submit" #> ajaxSubmitOnClick(() => onSubmit())
//-  }
//-
//-  def validateToken_!(): Unit =
//-    db().io.trans(DbLogic.user.findConfirmationTokenIssuedDate(token)).unsafePerformIO() match {
//-      case None =>
//-        S.error("Invalid registration token. Please re-register your email address.")
//-        redirectTo(AppSiteMap.Register1)
//-
//-      case Some(issued) if isTokenExpired(issued) =>
//-        S.error("Your registration token has expired. Please re-register your email address to get a new token.")
//-        redirectTo(AppSiteMap.Register1)
//-
//-      case _ => () // valid
//-    }
//-
//-  def onSubmit(): JsCmd =
//-    try {
//-      import UserRegistrationResult._
//-
//-      handleCompositeInvalidity(form validate vars)(r => {
//-        val (name, username, password, newsletter, _) = r
//-        val ps = PasswordAndSalt.createWithRandomSalt(password)
//-
//-        val dbPlan = DbLogic.user.performRegistration(token)(username, ps, clientIp().getOrElse("?"))(name, newsletter)
//-        db().io.trans(dbPlan).unsafePerformIO() match {
//-
//-          case UsernameTaken =>
//-            jsShowError("Username is already taken.")
//-
//-          case NoMatchingConfToken =>
//-            S.error("Your registration token disappeared.")
//-            redirectTo(AppSiteMap.Login)
//-
//-          // Registration complete
//-          case DbSuccess(id) =>
//-            log.info(s"Registered new user: $username")
//-            taskman().submitMsg(Msg.RegistrationCompleted(id)).unsafePerformIO()
//-            SecurityUtils.getSubject.login(new UsernamePasswordToken(username.value, password))
//-            JqExpr("#regComplete,#register2") ~> JqToggle
//-        }
//-      })
//-    } finally
//-      vars = vars put3 FormVar.emptyPasswordPair // Let's not keep the plaintext passwords around
        ???
      )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    val loginFn: F[Login.Fn.Instance] =
      svr.createServerSideProc(Login.Fn)(
        security.protectFn(
          _.validate.onValid(req => // TODO Why bother?
            security.attemptLogin(req.user, req.password))))

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    val resetPasswordFn1: F[ResetPassword.Fn1.Instance] =
      svr.createServerSideProc(ResetPassword.Fn1)(i =>
//-    def onSubmit(): JsCmd = {
//-      securityProvider().enforceHumanSpeed()
//-      perform(form validate vars)
//-    }
//-
//-    form.csssel(vars, vars = _) & ":submit" #> ajaxSubmitOnClick(() => onSubmit())
//-  }
//-
//-  def perform(v: Invalidity \/ EmailAddr): JsCmd =
//-    handleCompositeInvalidity(v) { email =>
//-
//-      val dbPlan = resetLogic(email).withTransactionLevel(Connection.TRANSACTION_SERIALIZABLE)
//-      val plan = db().io.trans(dbPlan).flatMap {
//-        case Some(msg) => taskman().submitMsg(msg).map(_ => ())
//-        case None => IO.ioUnit
//-      }
//-      plan.unsafePerformIO()
//-
//-      // Respond the same in all cases (for security purposes)
//-      jsEmailSent
//-    }
//-
//-  def resetLogic(email: EmailAddr): ConnectionIO[Option[Msg]] =
//-    DbLogic.user.findRegAndResetPwInfo(email) flatMap {
//-
//-      // No associated account
//-      case None =>
//-        Free pure None
//-
//-      // Account not activated yet
//-      case Some((u@UserRegistrationInfo(_, _, _, None), _)) =>
//-        Register1.preRegistrationMsg(email, u).map(Some(_))
//-
//-      // Valid token available
//-      case Some((UserRegistrationInfo(id, _, _, Some(_)), ResetPasswordInfo(Some(token), Some(issued)))) if !isTokenExpired(issued) =>
//-        reuseToken(id).map(_ => Some(passwordResetMsg(email, token)))
//-
//-      // No token or token expired
//-      case Some((UserRegistrationInfo(id, _, _, Some(_)), _)) =>
//-        issueNewToken(id).map(token => Some(passwordResetMsg(email, token)))
//-    }
//-
//-  def passwordResetMsg(email: EmailAddr, token: String): Msg =
//-    Msg.PasswordResetRequested(email, AppSiteMap.ResetPassword2.absoluteUrl(token))
//-
//-  val jsEmailSent: JsCmd =
//-    JqExpr("#resetpw1Form,#resetpwTokenSent") ~> JqToggle
//-
//-  private def issueNewToken(id: UserId): ConnectionIO[String] =
//-    DbLogic.user.performInstallNewResetPasswordToken(id, () => randomConfirmationToken())
//-
//-  private def reuseToken(id: UserId): ConnectionIO[Unit] =
//-    DbLogic.user.performReuseResetPasswordToken(id)
        ???
      )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    val resetPasswordFn2: F[ResetPassword.Fn2.Instance] =
      svr.createServerSideProc(ResetPassword.Fn2)(i =>
//-  def isTokenExpired(dateIssued: Instant): Boolean =
//-    Misc.isExpired_?(dateIssued, DI.serverConfig.passwordResetTokenLifespan)
//-  def validateToken_!(): Unit =
//-    db().io.trans(DbLogic.user.findResetPasswordTokenIssuedDate(token)).unsafePerformIO() match {
//-      case None =>
//-        S.error("The token associated with that URL is invalid.")
//-        redirectTo(AppSiteMap.Login)
//-
//-      case Some(issued) if isTokenExpired(issued) =>
//-        S.error("Your password-reset token has expired. Please re-enter your email address to get a new token.")
//-        redirectTo(AppSiteMap.ResetPassword1)
//-
//-      case _ => // valid
//-    }
//-
//-  def render = {
//-    validateToken_!()
//-    form.csssel(vars, vars = _) & ":submit" #> ajaxSubmitOnClick(() => onSubmit())
//-  }
//-
//-  def onSubmit(): JsCmd =
//-    try
//-      handleCompositeInvalidity(form validate vars)(resetPassword)
//-    finally
//-      vars = FormVar.emptyPasswordPair // Let's not keep the plaintext passwords around
//-
//-  def resetPassword(password: String): JsCmd = {
//-    val ps = PasswordAndSalt.createWithRandomSalt(password)
//-    db().io.trans(DbLogic.user.performPasswordReset(ps, token)).unsafePerformIO()
//-    jsClearError & JqExpr("#resetpw2Form,#resetpwComplete") ~> JqToggle
        ???
      )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    new PublicSpaLogic[F] {
      val initData: F[InitData] =
        for {
          a <- landingPageFn
          b <- registerFn1
          c <- registerFn2
          d <- loginFn
          e <- resetPasswordFn1
          f <- resetPasswordFn2
        } yield InitData(a, config.allowRegister, b, c, d, e, f)
    }
  }
}

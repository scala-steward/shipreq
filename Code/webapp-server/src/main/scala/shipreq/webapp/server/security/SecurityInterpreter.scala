package shipreq.webapp.server.security

import org.apache.shiro.authc.AuthenticationException
import scalaz.syntax.monad._
import scalaz.{Monad, \/}
import shipreq.webapp.base.user._
import shipreq.webapp.server.ServerConfig
import shipreq.webapp.server.logic._

final class SecurityInterpreter[F[_]](implicit F     : Monad[F],
                                               config: ServerConfig,
                                               ops   : OpsLogic[F],
                                               secDb : DB.ForSecurity[F],
                                               svr   : Server.Session[F],
                                               trace : Trace.Basic[F]) extends Security.Algebra[F] {

  override val db = secDb

  private[this] val fUnit = F.point(())

  private[this] val delay: F[Unit] =
    config.attackFrustrationDelayMs match {
      case 0  => fUnit
      case ms => val f = F.point(Thread.sleep(ms)); trace.sub("Security delay")(f)
    }

  override def protect[A](vulnerable: F[A]): F[A] =
    delay >> vulnerable

  override def attemptLogin(user: Username \/ EmailAddr, password: PlainTextPassword) = {
    val login: F[Option[User]] =
      F.point {
        val userOrEmail = user.fold(_.value, _.value)
        try {
          AppSecurityRealm.loginOrThrow(userOrEmail, password)
          AppSecurityRealm.authenticatedUser()
        } catch {
          case _: AuthenticationException => None
        }
      }

    def trackLogin(user: User): F[Unit] =
      svr.sessionId.flatMap(_.fold(fUnit)(ops.trackLogin(_, user)))

    for {
      ou <- login
      _  <- ou.fold(fUnit)(trackLogin)
    } yield ou
  }

  private[this] val hashFn = AppSecurityRealm.randomHashFn

  override def hashPassword(p: PlainTextPassword): F[PasswordAndSalt] =
    F.point(hashFn(p))

  override val isAuthenticated: F[Boolean] =
    F.point(AppSecurityRealm.isAuthenticated())

  override val authenticatedUser: F[Option[User]] =
    F.point(AppSecurityRealm.authenticatedUser())

  override val logout: F[Unit] = {
    val updateShiro = F.point(AppSecurityRealm.logout())
    val updateOps   = svr.sessionId.flatMap(_.fold(fUnit)(ops.trackLogout))
    updateShiro >> updateOps
  }
}

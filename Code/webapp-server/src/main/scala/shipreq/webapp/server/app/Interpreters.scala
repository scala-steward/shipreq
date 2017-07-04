package shipreq.webapp.server.app

import doobie.imports.ConnectionIO
import java.time.{Duration, Instant}
import net.liftweb.actor.LAScheduler
import nyaya.gen.Gen
import scalaz.{-\/, \/, \/-}
import scalaz.effect.IO
import scalaz.syntax.all._
import scalaz.~>
import shipreq.base.db.DbAccess
import shipreq.base.db.DoobieHelpers._
import shipreq.base.util.Permission
import shipreq.webapp.base.data._
import shipreq.webapp.base.event.{ActiveEvent, EventOrd}
import shipreq.webapp.base.hash.HashRec
import shipreq.webapp.base.protocol.ServerSideProc
import shipreq.webapp.base.user._
import shipreq.webapp.server.db.DbLogic
import shipreq.webapp.server.ServerConfig
import shipreq.webapp.server.logic._
import shipreq.webapp.server.protocol.ServerProtocol
import shipreq.webapp.server.security.AppSecurityRealm

object Interpreters {

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  implicit def dbAlgebra(implicit config: ServerConfig): DB.Algebra[ConnectionIO] = // TODO Replace DbLogic
    new DB.Algebra[ConnectionIO] {
      import DB._

      val tokenGen: () => SecurityToken = {
        val it = Gen.alphaNumeric.samples()
        val size = config.securityTokenLength
        () => {
          val sb = new StringBuilder(size)
          var i = size
          while (i > 0) {
            i -= 1
            sb.append(it.next())
          }
          SecurityToken(sb.result())
        }
      }

      override def inDbTransaction[A](f: ConnectionIO[A]): ConnectionIO[A] =
        f.inTransaction

      override def inDbTransaction[A](level: Int, f: ConnectionIO[A]): ConnectionIO[A] =
        f.inTransaction.withTransactionLevel(level)

      override def getUserRegistration(e: EmailAddr): ConnectionIO[Option[UserRegistration]] =
        DbLogic.user.findRegistrationInfo(e)

      /** Creates an unconfirmed user account. No username, no password until email confirmed. */
      override def createUserPlaceholder(e: EmailAddr): ConnectionIO[SecurityToken] =
        DbLogic.user.createPlaceholder(e, tokenGen)

      override def updateUserRegistrationToken(id: UserId): ConnectionIO[SecurityToken] =
        DbLogic.user.updateConfirmationToken(id, tokenGen)

      override def getUserRegistrationTokenIssueDate(t: SecurityToken): ConnectionIO[Option[Instant]] =
        DbLogic.user.findConfirmationTokenIssuedDate(t)

      override def completeUserRegistration(token     : SecurityToken,
                                            name      : PersonName,
                                            username  : Username,
                                            ps        : PasswordAndSalt,
                                            newsletter: Boolean,
                                            ip        : Option[IP]): ConnectionIO[UserRegistrationResult] =
        DbLogic.user.performRegistration(token, name, username, ps, newsletter, ip)

      override def getPasswordResetState(ue: Username \/ EmailAddr): ConnectionIO[Option[(EmailAddr, PasswordResetState)]] =
        ue match {
          case \/-(e) => DbLogic.user.getPasswordResetStateByEmail(e).map(_.map((e, _)))
          case -\/(u) => DbLogic.user.getPasswordResetStateByUsername(u)
        }

      override def getResetPasswordTokenIssueDate(t: SecurityToken): ConnectionIO[Option[Instant]] =
        DbLogic.user.findResetPasswordTokenIssuedDate(t)

      override def createResetPasswordToken(id: UserId): ConnectionIO[SecurityToken] =
        DbLogic.user.performInstallNewResetPasswordToken(id, tokenGen)

      /** Updates the sent-count and sent-at attributes of an existing reset-password token. */
      override def updateResetPasswordTokenOnReissue(id: UserId): ConnectionIO[Unit] =
        DbLogic.user.performReuseResetPasswordToken(id)

      /** This also clears the token */
      override def updateUserPassword(token: SecurityToken, ps: PasswordAndSalt): ConnectionIO[Unit] =
        DbLogic.user.performPasswordReset(token, ps)

      override def createEmptyProject(id: UserId): ConnectionIO[ProjectId] =
        DbLogic.project.create(id)

      override def getProjectHeader(id: ProjectId): ConnectionIO[Option[ProjectHeader]] =
        DbLogic.project.findProjectHeader(id)

      override def getProjectMetaData(id: ProjectId): ConnectionIO[Option[ProjectMetaData]] =
        DbLogic.project.findProjectMetaData(id)

      override def getAllProjectEvents(id: ProjectId): ConnectionIO[ProjectEvents] =
        DbLogic.event.findAll2(id)

      override def getAllProjectMetaDataForUser(id: UserId): ConnectionIO[List[ProjectMetaData]] =
        DbLogic.project.findAllProjectMetaDataForUser(id)

      override def saveProjectEvent(id: ProjectId)(o: EventOrd, e: ActiveEvent, h: HashRec.Collection): ConnectionIO[Option[Throwable]] =
        DbLogic.event.create(id, o, e, h).attempt.map(_.fold[Option[Throwable]](Some(_), _ => None))
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  implicit def runDB(implicit dbAccess: DbAccess): ConnectionIO ~> IO =
    new (ConnectionIO ~> IO) {
      override def apply[A](fa: ConnectionIO[A]) = dbAccess.io.trans(fa)
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  implicit val serverAlgebra: Server.Algebra[IO] =
    new Server.Algebra[IO] {
      import net.liftweb.common._
      import net.liftweb.http.S

      override def createServerSideProc(p: ServerSideProc.Protocol)(localFn: p.Input => IO[p.Response]): IO[p.Instance] =
        IO(ServerProtocol.createServerSideProc(p)(localFn(_)))

      override val now: IO[Instant] =
        IO(Instant.now())

      override def delay[A](f: IO[A], d: Duration): IO[A] =
        IO(Thread.sleep(d.toMillis)) >> f // TODO Thread.sleep lolz

      override def fork[A](f: IO[A]): IO[Unit] =
        IO(LAScheduler.execute(() => f.unsafePerformIO()))

      override val clientIP: IO[Option[IP]] =
        IO {
          // println("X-Real-IP: " + req.header("X-Real-IP"))
          // println("X-Forwarded-For: " + req.header("X-Forwarded-For"))
          val box: Box[String] =
            S.originalRequest.filter(_.request ne null).map(_.remoteAddr) or
              S.containerRequest.map(_.remoteAddress) or
              S.request.filter(_.request ne null).map(_.remoteAddr)

          box match {
            case Full(ip) => Some(IP(ip))
            case _        => None
          }
        }
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  implicit def securityAlgebra(implicit config: ServerConfig): Security.Algebra[IO] =
    new Security.Algebra[IO] {

      val delay: IO[Unit] =
        config.attackFrustrationDelayMs match {
          case 0  => IO(())
          case ms => IO(Thread.sleep(ms))
        }

      override def protect[A](vulnerable: IO[A]): IO[A] =
        delay >> vulnerable

      override def attemptLogin(user: Username \/ EmailAddr, password: PlainTextPassword): IO[Permission] =
        IO(AppSecurityRealm.attemptLogin(user.fold(_.value, _.value), password))

      val hashFn = AppSecurityRealm.randomHashFn
      override def hashPassword(p: PlainTextPassword): IO[PasswordAndSalt] =
        IO(hashFn(p))
    }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  implicit val projectStore: ProjectServer.StoreAlgebra[IO] =
    Store.Algebra.concurrentHashMap()
}

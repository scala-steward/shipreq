package shipreq.webapp.server.logic

import shipreq.webapp.base.user.User

trait MetricsLogic[F[_]] {

  // {HttpRequests, HttpIO, HttpDuration} done directly in webapp-server

  def sessionStart(sessionId: SessionId): F[Unit]

  def sessionEnd(sessionId: SessionId): F[Unit]

  def login(sessionId: SessionId, user: User): F[Unit]

  def logout(sessionId: SessionId): F[Unit]

//    val ProjectsActive =
//      Gauge.build(prefix + "projects_active", "Projects currently being served")
}

object MetricsLogic {

  def const[F[_]](f: F[Unit]): MetricsLogic[F] =
    new MetricsLogic[F] {
      override def sessionStart(sessionId: SessionId) = f
      override def sessionEnd(sessionId: SessionId) = f
      override def login(sessionId: SessionId, user: User) = f
      override def logout(sessionId: SessionId) = f
    }
}
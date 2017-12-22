package shipreq.webapp.server.app

import nyaya.gen.Gen
import shipreq.base.util.FxModule._
import shipreq.taskman.api.TaskmanApi
import shipreq.webapp.base.user.User
import shipreq.webapp.server.logic.{DB, OpsLogic, Server, SessionId}

final class OpsInterpreter(implicit
                           db: DB.ForOps[Fx],
                           svr: Server.Time[Fx],
                           taskman: TaskmanApi[Fx])
  extends OpsLogic.Base[Fx]()(implicitly, db, svr, taskman) {

  val sessionTracker = new SessionTracker

  override def trackLogin(sessionId: SessionId, user: User) =
    Fx(sessionTracker.login(sessionId, user))

  override def trackLogout(sessionId: SessionId) =
    Fx(sessionTracker.logout(sessionId))

  override val sessionStats =
    Fx(OpsLogic.SessionStats(
      active      = sessionTracker.activeSessionCount(),
      loggedIn    = sessionTracker.loggedInSessionCount(),
      uniqueUsers = sessionTracker.uniqueUserCount(),
      timeout     = sessionTracker.timeout()))

  override protected val randomToken =
    Fx(Gen.alphaNumeric.string(16).sample())

}

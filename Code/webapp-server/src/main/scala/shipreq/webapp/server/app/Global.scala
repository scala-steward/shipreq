package shipreq.webapp.server.app

import doobie.imports.ConnectionIO
import scalaz.effect.IO
import shipreq.base.db.DbAccess
import shipreq.taskman.api.TaskmanApi
import shipreq.taskman.api.impl.TaskmanApiImpl
import shipreq.webapp.server.ServerConfig
import shipreq.webapp.server.logic.{ProjectServer, ServerLogic}
import shipreq.webapp.server.security.AppSecurityRealm

final case class Global(config  : ServerConfig,
                        db      : DbAccess,
                        logic   : ServerLogic[IO],
                        taskman : TaskmanApi[IO]) {

  // TODO Delete Global.security
  object security {
    def loggedInUser()    = AppSecurityRealm.authenticatedUser()
    def logout()          = AppSecurityRealm.logout()
    def isAuthenticated() = AppSecurityRealm.isAuthenticated()
  }
}

object Global {
  var Instance: Global = _

  @inline implicit def autoInstance(g: Global.type): Global = Instance

  def modify(f: Global => Global): Unit =
    Instance = f(Instance)

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  def default(implicit db: DbAccess, config: ServerConfig): Global = {
    assert(db ne null, "DbAccess is null, sir.")
    val taskmanCtx = TaskmanApiImpl.Context(Some(config.taskmanSchema))
    implicit val taskman = TaskmanApiImpl(taskmanCtx, db.io.trans)
    import Interpreters._
    Global(
      config   = config,
      db       = db,
      logic    = ServerLogic.create[ConnectionIO, IO](ProjectServer.BroadcastTo.All),
      taskman  = taskman)
    }
}
package shipreq.webapp.server.test

import java.time.Duration
import shipreq.webapp.server.app.Global

object PrepareEnv {
  private val boot = new bootstrap.liftweb.Boot

  private lazy val cfg = {
    val (appConfig, runMode) = boot.readConfig()
    runMode foreach boot.setRunMode
    println("webapp-server test config:\n" + appConfig.report.reportUsed)
    appConfig
  }

  private def once[A](a: => A): () => Unit = {
    lazy val o = {a; ()}
    () => o
  }

  Global.Instance = Global(
    config  = cfg.server.copy(attackFrustrationDelay = Duration.ZERO),
    db      = null,
    logic   = null,
    taskman = null)

  val shiro: () => Unit = once {
    boot.initShiro()
  }

  val lift: () => Unit = once {
    // if (!LiftRules.doneBoot) {
    shiro()
    boot.configureLift()
    boot.preloadTemplates()
  }

  def db(): Unit = {
    TestDb.init()
    TestDb.useInLift()
  }
}

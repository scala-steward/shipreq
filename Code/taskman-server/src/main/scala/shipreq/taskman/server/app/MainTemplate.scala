package shipreq.taskman.server.app

import scalaz.syntax.applicative._
import shipreq.base.db.{DbAccess, DbConfig}
import shipreq.base.ops.{JdbcLogging, SqlTracer}
import shipreq.base.util.FxModule._
import shipreq.base.util.Props
import shipreq.base.util.log.HasLogger
import shipreq.taskman.server.{TaskmanConfig, TaskmanCtx}
import DbAccess.fxCapture

private[app] trait MainTemplate extends HasLogger {

  def withDatabase[A](f: DbAccess => Fx[A]): Fx[A] =
    for {
      (cfg, report) <- DbConfig.config.withReport.run(Props.sources).map(_.getOrDie)
      _             <- Fx(logger.info(s"Config report:\n${report.full}"))
      dbAccess      <- dbInit(cfg)
      a             <- dbAccess.setupRunShutdown(f(dbAccess))
    } yield a

  def withTaskmanCtx[A](f: TaskmanCtx => Fx[A]): Fx[A] = {
    val readConfig = (DbConfig.config tuple TaskmanConfig.config).withReport.run(Props.sources)

    def onShutDown(dbAccess: DbAccess, taskmanCfg: TaskmanConfig) =
      for {
      ctx <- Fx(TaskmanCtx(dbAccess, taskmanCfg))
      a <- f(ctx) andFinally ctx.shutdown
    } yield a

    for {
      ((dbCfg, taskmanCfg), report) <- readConfig.map(_.getOrDie)
      _                             <- Fx(logger.info(report.full))
      dbAccess                      <- dbInit(dbCfg)
      a                             <- dbAccess.setupRunShutdown(onShutDown(dbAccess, taskmanCfg))
    } yield a
  }

  private def dbInit(dbCfg: DbConfig): Fx[DbAccess] = {
//    val sqlTracer: SqlTracer = JdbcLogging
//    if (cfg.server.prometheus.enabled && cfg.server.prometheus.jdbc)
//      sqlTracer = sqlTracer compose JdbcMetrics.sqlTracer("webapp")
//    dbCfg.modifyHikariDataSource(sqlTracer.inject)
    DbAccess.fromCfg(dbCfg)
  }

}

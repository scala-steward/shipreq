package shipreq.taskman.server.app

import scalaz.effect.IO
import scalaz.syntax.applicative._
import shipreq.base.db.{DbAccess, DbConfig}
import shipreq.base.util.Props
import shipreq.base.util.log.HasLogger
import shipreq.taskman.server.{TaskmanConfig, TaskmanCtx}

private[app] trait MainTemplate extends HasLogger {

  def withDatabase[A](f: DbAccess => IO[A]): IO[A] =
    for {
      tmp <- DbConfig.config.withReport.run(Props.sources).map(_.getOrDie)
      (cfg, report) = tmp
      _ <- IO(log.info(report.report))
      dbAccess <- DbAccess.fromCfg(cfg)
      a <- dbAccess.setupRunShutdown(f(dbAccess))
    } yield a

  def withTaskmanCtx[A](f: TaskmanCtx => IO[A]): IO[A] =
    for {
      tmp <- (DbConfig.config tuple TaskmanConfig.config).withReport.run(Props.sources).map(_.getOrDie)
      ((dbCfg, taskmanCfg), report) = tmp
      _ <- IO(log.info(report.report))
      dbAccess <- DbAccess.fromCfg(dbCfg)
      a <- dbAccess.setupRunShutdown(
        for {
          ctx <- IO(TaskmanCtx(dbAccess, taskmanCfg))
          a <- f(ctx) ensuring ctx.shutdown
        } yield a
      )
    } yield a
}

package shipreq.taskman.api

import scalaz.{Applicative, Monad, Traverse, ~>}
import scalaz.syntax.monad.ToBindOps
import scalaz.syntax.traverse._
import shipreq.base.util.FxModule._
import shipreq.base.util.log.HasLogger

trait TaskmanApi[F[_]] { self =>

  /**
   * Stores/updates a configuration value that will be read by the Taskman server.
   *
   * @param key The config key.
   * @param value The config value.
   */
  def cfgPut(key: String, value: String): F[Unit]

  /** Submits a Msg to the Taskman server for processing. */
  def submitMsg(m: Msg): F[MsgId]

  /** Inspects the status of a msg. */
  def queryMsgStatus(id: MsgId): F[Option[MsgStatus]]

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  def cfgPutBulk(kvs: (String, String)*)(implicit F: Monad[F]): F[Unit] =
    if (kvs.isEmpty)
      F.pure(())
    else
      kvs.iterator.map(kv => cfgPut(kv._1, kv._2)).reduce(_ >> _)

  /** Submits 0-n Msgs to the Taskman server for processing. */
  def submitMsgs[G[_] : Traverse](ms: G[Msg])(implicit F: Applicative[F]): F[G[(Msg, MsgId)]] =
    ms.traverse(m => submitMsg(m).map((m, _)))

  /** Submits 0-n Msgs to the Taskman server for processing, discarding the results. */
  def submitMsgs_[G[_] : Traverse](ms: G[Msg])(implicit F: Applicative[F]): F[Unit] =
    ms.traverse_(submitMsg(_).void)

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final def trans[G[_]](g: F ~> G)(implicit F: Monad[F]): TaskmanApi[G] =
    new TaskmanApi[G] {
      override def cfgPut(k: String, v: String) = g(self.cfgPut(k, v))
      override def submitMsg(m: Msg)            = g(self.submitMsg(m))
      override def queryMsgStatus(id: MsgId)    = g(self.queryMsgStatus(id))

      // In practice, these override merge actions into a single DB transaction
      override def cfgPutBulk(kvs: (String, String)*)(implicit G: Monad[G])             = g(self.cfgPutBulk(kvs: _*))
      override def submitMsgs[H[_] : Traverse](ms: H[Msg])(implicit G: Applicative[G])  = g(self.submitMsgs(ms))
      override def submitMsgs_[H[_] : Traverse](ms: H[Msg])(implicit G: Applicative[G]) = g(self.submitMsgs_(ms))
    }
}

object TaskmanApi extends HasLogger {
  def addLogging(self: TaskmanApi[Fx]): TaskmanApi[Fx] =
    new TaskmanApi[Fx] {
      override def cfgPut(k: String, v: String) =
        for {
          (_, dur) <- self.cfgPut(k, v).measureDuration
          _        <- Fx(logger.info(s"Put config {$k=$v} in Taskman in ${dur.toMillis} ms"))
        } yield ()

      override def submitMsg(m: Msg) =
        for {
          (id, dur) <- self.submitMsg(m).measureDuration
          _         <- Fx(logger.info(s"Submitted $m to Taskman in ${dur.toMillis} ms"))
        } yield id

      override def queryMsgStatus(id: MsgId) =
        for {
          (os, dur) <- self.queryMsgStatus(id).measureDuration
          _         <- Fx(logger.info(s"Retrieved $id status as $os from Taskman in ${dur.toMillis} ms"))
        } yield os
    }
}
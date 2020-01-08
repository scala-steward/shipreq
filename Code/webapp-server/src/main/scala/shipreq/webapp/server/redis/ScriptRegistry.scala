package shipreq.webapp.server.redis

import com.typesafe.scalalogging.StrictLogging
import scala.collection.mutable
import shipreq.base.util.FxModule._

final class ScriptRegistry extends StrictLogging {
  private var deploys = Vector.empty[Fx[ScriptSha]]
  private var shas = mutable.ArrayBuffer.empty[Option[ScriptSha]]
  private val lock = new AnyRef

  private def unsafeGetSha(idx: Int): ScriptSha =
    lock.synchronized(shas(idx))
      .getOrElse {
        val sha = lock.synchronized(deploys(idx)).unsafeRun()
        lock.synchronized { shas(idx) = Some(sha) }
        sha
      }

  private def unsafeForgetAllShas(): Unit =
    lock.synchronized {
      for (i <- shas.indices)
        shas(i) = None
    }

  def register[I, O](deploy: Fx[ScriptSha])
                    (run: (ScriptSha, I) => Fx[O]): I => Fx[O] = {

    val initialSha = deploy.unsafeRun()

    val idx =
      lock.synchronized {
        deploys :+= deploy
        shas :+= Some(initialSha)
        deploys.length - 1
      }

    i =>
      Fx {
        try {
          val sha = unsafeGetSha(idx)
          run(sha, i).unsafeRun()
        } catch {
          case e: Exception if e.getMessage != null && e.getMessage.contains("NOSCRIPT") =>
            // Redis has been restarted
            unsafeForgetAllShas()
            logger.warn("Redis script lost! Redeploying...")
            val sha = unsafeGetSha(idx)
            run(sha, i).unsafeRun()
        }
      }
  }
}
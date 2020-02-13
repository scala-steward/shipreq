package shipreq.base.util

import java.util.concurrent.{Executors, TimeUnit}
import java.time.Duration

object ThreadUtils {
  import FxModule._

  object ThreadGroups {
    val scheduledTasks = new ThreadGroup("ScheduledTasks")
  }

  def newScheduler(threadName: String, threadGroup: ThreadGroup): Scheduler =
    new Scheduler(threadName, threadGroup)

  final class Scheduler(threadName: String, threadGroup: ThreadGroup) {
    private val es = Executors.newSingleThreadScheduledExecutor(new Thread(threadGroup, _, threadName))

    def scheduleAtFixedRate[A](fx: Fx[A], period: Duration): Scheduler =
      scheduleAtFixedRate(fx, period, period)

    def scheduleAtFixedRate[A](fx: Fx[A], initialDelay: Duration, period: Duration): Scheduler = {
      es.scheduleAtFixedRate(fx.toJavaRunnable, initialDelay.toMillis, period.toMillis, TimeUnit.MILLISECONDS)
      this
    }

    def addShutdownHook[A](fx: Fx[A]): Scheduler = {
      val t = new Thread(threadGroup, fx.toJavaRunnable, "ReportDaoBatchPostgres-shutdown")
      Runtime.getRuntime.addShutdownHook(t)
      this
    }
  }
}

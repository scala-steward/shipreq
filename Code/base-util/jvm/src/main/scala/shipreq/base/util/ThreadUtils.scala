package shipreq.base.util

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import java.time.Duration

object ThreadUtils {
  import FxModule._

  object ThreadGroups {
    val scheduledTasks = new ThreadGroup("ScheduledTasks")
  }

  def newScheduler(threadName: String, threadGroup: ThreadGroup): ScheduledExecutorServiceUtil = {
    val es = Executors.newSingleThreadScheduledExecutor(new Thread(threadGroup, _, threadName))
    new ScheduledExecutorServiceUtil(es)
  }

  final class ScheduledExecutorServiceUtil(private val self: ScheduledExecutorService) extends AnyVal {

    def scheduleAtFixedRate[A](fx: Fx[A], period: Duration): ScheduledExecutorServiceUtil =
      scheduleAtFixedRate(fx, period, period)

    def scheduleAtFixedRate[A](fx: Fx[A], initialDelay: Duration, period: Duration): ScheduledExecutorServiceUtil = {
      self.scheduleAtFixedRate(fx.toJavaRunnable, initialDelay.toMillis, period.toMillis, TimeUnit.MILLISECONDS)
      this
    }
  }
}

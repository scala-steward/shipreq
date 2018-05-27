package shipreq.base.ops

import io.prometheus.client.{Histogram, SimpleTimer}

object JdbcMetrics {

  private[JdbcMetrics] object Label {
    final val App     = "app"
    final val Method  = "method"
    final val Sql     = "sql"
    final val Batches = "batches"
    final val Success = "success"
  }

  private[JdbcMetrics] val Duration =
    Histogram.build("jdbc_duration_seconds", "JDBC call durations")
      .buckets(.002, .004, .006, .008, .01, .015, .02, .025, .03, .04, .05, .075, .1, .25, .5, .75, 1, 2, 4, 8)
      .labelNames(Label.App, Label.Method, Label.Sql, Label.Batches, Label.Success)
      .register()

  @inline private def yesOrNo(yes: Boolean): String =
    if (yes) "y" else "n"

  def sqlTracer(app: String): SqlTracer =
    new SqlTracer {
      override def logExecute(method: String, sql: String, batches: Int, err: Option[Throwable], startTimeNs: Long, endTimeNs: Long): Unit = {
        val dur = SimpleTimer.elapsedSecondsFromNanos(startTimeNs, endTimeNs)
        val success = yesOrNo(err.isEmpty)
        Duration.labels(app, method, sql, batches.toString, success).observe(dur)
      }
    }

}

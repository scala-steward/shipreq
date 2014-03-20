package shipreq.taskman.server.business

import org.joda.time.Period
import scalaz.Scalaz.none
import shipreq.taskman.api.Priority
import shipreq.taskman.server.Deterministic
import shipreq.taskman.server.Worker.{FailurePolicy, FailureResponse, FailureCtx}
import shipreq.taskman.server.Sop._

object Failure {
  implicit class TimeHelpers(val n: Int) extends AnyVal {
    def second  = Period seconds n
    def seconds = Period seconds n
    def minute  = Period minutes n
    def minutes = Period minutes n
    def hour    = Period hours n
    def hours   = Period hours n
    def day     = Period days n
    def days    = Period days n
  }

  type Rule = FailureCtx => Option[FailureResponse]

  type RetryRule = FailureCtx => Option[Period]

  private[this] def ifO[A](p: Boolean)(r: => A): Option[A] =
    if (p) Some(r) else None

  private[this] def chooseFirst[A, B](fs: (A => Option[B])*): A => Option[B] = a =>
    (none[B] /: fs)((pref,f) => pref orElse f(a))

  private[this] def chooseFirstOr[A, B](fs: (A => Option[B])*)(fallback: A => B): A => B = a =>
    chooseFirst(fs: _*)(a) getOrElse fallback(a)

  def retryTable(x: IndexedSeq[Period]): RetryRule = {
    val v = x.map(Some(_)).toVector
    ctx => {
      val i: Int = ctx.m.failureCount
      if (i >= v.length) None else v(i)
    }
  }

  def retryEveryUntil(every: Period, cutoff: Period): RetryRule = {
    val everyS = Some(every)
    ctx => {
      val retryExpiry = ctx.m.h.created plus cutoff
      if (ctx.now.isAfter(retryExpiry)) None else everyS
    }
  }

  def runRetry(retry: RetryRule): Rule = ctx =>
    retry(ctx).map(delay => FailureResponse(MsgFailedRetry(ctx.m, delay), Nil))

  def notifyOfFailure(ctx: FailureCtx) = NotifySupportWorkerFailed(ctx.m, ctx.err)

  val abortAndNotify: FailurePolicy = ctx =>
    FailureResponse(MsgFailedAbort(ctx.m), notifyOfFailure(ctx) :: Nil)

  def notifyOnFailure(f: Rule): Rule = ctx =>
    f(ctx).map(r => r.copy(additionalOps = notifyOfFailure(ctx) :: r.additionalOps))

  // ===================================================================================================================

  def abortDeterministicErrors: Rule = ctx =>
    ifO(ctx.err is Deterministic)(abortAndNotify(ctx))

  val impatientRetries = chooseFirst(
    retryTable(Vector(
      10 seconds    // Failure #1,  next attempt @ 10 sec
      , 15 seconds  // Failure #2,  next attempt @ 25 sec
      , 20 seconds  // Failure #3,  next attempt @ 45 sec
      , 35 seconds  // Failure #4,  next attempt @ 80 sec
      , 40 seconds  // Failure #5,  next attempt @ 2 min
      , 60 seconds  // Failure #6,  next attempt @ 3 min
      , 90 seconds  // Failure #7,  next attempt @ 4.5 min
      , 150 seconds // Failure #8,  next attempt @ 7 min
      , 4 minutes   // Failure #9,  next attempt @ 11 min
      , 6 minutes   // Failure #10, next attempt @ 17 min
      , 13 minutes  // Failure #11, next attempt @ 30 min
      , 20 minutes  // Failure #12, next attempt @ 50 min
      , 30 minutes  // Failure #13, next attempt @ 80 min
      , 40 minutes  // Failure #14, next attempt @ 2 hr
    )),
    retryEveryUntil(1 hour, 24 hours)
  )

  val patientRetries = chooseFirst(
    retryTable(Vector(
      30 seconds   // Failure #1, next attempt @ 30 sec
      , 90 seconds // Failure #2, next attempt @  2 min
      , 3 minutes  // Failure #3, next attempt @  5 min
      , 5 minutes  // Failure #4, next attempt @ 10 min
      , 10 minutes // Failure #5, next attempt @ 20 min
      , 15 minutes // Failure #6, next attempt @ 35 min
      , 25 minutes // Failure #7, next attempt @  1 hr
      , 1 hour     // Failure #8, next attempt @  2 hr
      , 2 hours    // Failure #9, next attempt @  4 hr
    )),
    retryEveryUntil(4 hours, 24 hours)
  )

  val retryAccordingToPriority: Rule = ctx => {
    val retryPolicy: RetryRule =
      if (ctx.m.h.p.value >= Priority.High.value)
        impatientRetries
      else
        patientRetries
    notifyOnFailure(runRetry(retryPolicy))(ctx)
  }

  val failurePolicy: FailurePolicy =
    chooseFirstOr(
      abortDeterministicErrors,
      retryAccordingToPriority
    )(abortAndNotify)
}

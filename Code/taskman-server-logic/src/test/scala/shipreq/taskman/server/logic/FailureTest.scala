package shipreq.taskman.server.logic

import japgolly.microlibs.stdlib_ext.StdlibExt._
import java.time.Duration
import shipreq.base.util.ArticulateError
import utest._
import Failure._
import ServerOp._
import TestHelpers._
import Worker._

object FailureTest extends TestSuite {

  private val genericError = ArticulateError("NO!")
  private val deterministicError = ArticulateError("ALWAYS NO!").tagDeterministic
  private val ctx_det = FailureCtx(node1, worker2, md_1, deterministicError, timeNow)
  private val ctx_nd = FailureCtx(node1, worker2, md_1, genericError, timeNow)

  implicit class Assertions(private val self: Option[FailureResponse]) extends AnyVal {
    def assertReactWith(f: FailedJobReaction) =
      assert(self.map(_.reaction) == Some(f))

    def assertRetryIn(d: Duration)(implicit c: FailureCtx) =
      assertReactWith(UpdateMsgRetry(c.node, c.worker, c.msg, d))

    def assertNotifySupport: Unit =
      assert(self.map(_.additionalOps).exists(_.count(_.isInstanceOf[NotifySupportWorkerFailed]) == 1))
  }

  override def tests = Tests {
    "abortDeterministicErrors" - {
      implicit val c = ctx_det
      val test = abortDeterministicErrors.partial

      "abort when error is deterministic" - {
        test(c).assertReactWith(UpdateMsgAbort(c.node, c.worker, c.msg))
      }

      "notify support when error is deterministic" - {
        test(c).assertNotifySupport
      }

      "pass through when error is not deterministic" - {
        test(ctx_nd) ==> None
      }
    }

    "retryAndNotify" - {
      val test = retryAndNotify.partial

      "on first failure, retry in 30s and notify support" - {
        implicit val c = lenses.failureCtx.failureCountL.set(ctx_nd, 0)
        val result = test(c)
        result.assertRetryIn(30 seconds)
        result.assertNotifySupport
      }

      "on second failure, retry in 90s and notify support" - {
        implicit val c = lenses.failureCtx.failureCountL.set(ctx_nd, 1)
        val result = test(c)
        result.assertRetryIn(90 seconds)
        result.assertNotifySupport
      }

      "on 20th failure before cutoff, retry in 4h and notify support" - {
        implicit val c = lenses.failureCtx.failureCountL.set(ctx_nd, 19)
        val result = test(c)
        result.assertRetryIn(4 hours)
        result.assertNotifySupport
      }

      "on 20th failure after cutoff, pass through" - {
        implicit val c = lenses.failureCtx.failureCountL.set(ctx_nd, 19).copy(now = timeNow plus 2.days)
        test(c) ==> None
      }
    }
  }
}


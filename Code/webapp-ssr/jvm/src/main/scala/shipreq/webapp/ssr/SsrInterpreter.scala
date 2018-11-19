package shipreq.webapp.ssr

import com.typesafe.scalalogging.StrictLogging
import japgolly.scalagraal._
import japgolly.scalagraal.GraalJs._
import japgolly.scalagraal.GraalBoopickle._
import scala.util.control.NonFatal
import shipreq.base.util.FxModule._
import SsrAlgebra.Types._
import shipreq.base.util.Url

final class SsrInterpreter(ctx: ContextSync) extends SsrAlgebra[Fx] with StrictLogging {

  private def samplePublicInitData: PublicInitData = {
    import shipreq.base.util.Allow
    import shipreq.webapp.base.protocol.{ServerSideProc, ServerSideProcId}
    import shipreq.webapp.client.public.PublicSpaProtocols._
    val sspId = ServerSideProcId("X")
    PublicInitData(
      publicRegistration = Allow,
      loggedInUser = None,
      landingPage = ServerSideProc(sspId, LandingPage.Fn),
      register1 = ServerSideProc(sspId, Register.Fn1),
      register2 = ServerSideProc(sspId, Register.Fn2),
      login = ServerSideProc(sspId, Login.Fn),
      resetPassword1 = ServerSideProc(sspId, ResetPassword.Fn1),
      resetPassword2 = ServerSideProc(sspId, ResetPassword.Fn2))
  }

  override def warmup = Fx {
    logger.info("Warming up SSR....")

    ctx.eval(setUrl("https://shipreq.com"))

    Warmup.sync(ctx)(10, publicExpr(samplePublicInitData), s => {
      logger.info(s"SSR $s")
      s.lastEvalAverage(10).millis < 40 || s.totalWarmupTime.seconds > 30 || s.totalInnerReps >= 1000
    })

    logger.info("Warming up done.")
  }

  private val setUrl = Expr.compileFnCall1[String]("setUrl")(identity)

  private def runner[A](name: String, expr: A => Expr[String]): (Url.Absolute, A) => Fx[Option[Html]] = {
    val logHead = s"Rendered $name in "
    val mw = ContextMetrics.Writer(s => logger.info(logHead + s.total.toStrMs))
    (url, a) => run(setUrl(url.absoluteUrl) >> expr(a), mw, name)
  }

  private def run(expr: Expr[String], mw: ContextMetrics.Writer, name: String): Fx[Option[Html]] =
    Fx {
      try
        ctx.eval(expr, mw) match {
          case Right(html) => Some(Html(html))
          case Left(e)     =>
            logger.warn(s"ExprError occurred rendering $name", e)
            None
        }
      catch {
        case NonFatal(t) =>
          logger.warn(s"Unhandled exception occurred rendering $name", t)
          None
      }
    }

  private val publicExpr = Expr.compileFnCall1[PublicInitData]("public")(_.asString)
  override val public = runner("public", publicExpr)
}

object SsrInterpreter {

  def apply(prometheus: Boolean): SsrInterpreter = {
    val setup = (
      Expr("window = {console: console, navigator: {userAgent:''}}")
       >> Expr.requireFileOnClasspath("webapp-ssr-deps.js")
       >> Expr.requireFileOnClasspath("webapp-ssr.js"))

    var ctxBuilder = ContextSync.Builder.fixedContext()
      .onContextCreate(setup)

    if (prometheus) {
      val w = GraalPrometheus.Builder().registerAndBuild()
      ctxBuilder = ctxBuilder.writeMetrics(w)
    }

    val ctx = ctxBuilder.build()

    new SsrInterpreter(ctx)
  }
}
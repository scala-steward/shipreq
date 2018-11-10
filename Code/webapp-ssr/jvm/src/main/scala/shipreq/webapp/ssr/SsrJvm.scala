package shipreq.webapp.ssr

import com.typesafe.scalalogging.StrictLogging
import japgolly.scalagraal._
import shipreq.webapp.client.public.PublicSpaProtocols.{InitData => PublicInitData}

object SsrJvm extends StrictLogging {
  import GraalJs._
  import GraalBoopickle._

  val lib1 = Expr.requireFileOnClasspath("webapp-ssr-deps.js")
  val lib2 = Expr.requireFileOnClasspath("webapp-ssr.js")
  private val ctx = {
    ContextSync()
//      .withAround(ContextSync.Around.before(lib2(_).left.toOption.foreach(e => throw e.underlying)))
//      .withAround(ContextSync.Around.before(lib1(_).left.toOption.foreach(e => throw e.underlying)))
  }

  private val exprPublic =
    Expr.compileFnCall1[PublicInitData]("public")(_.asString.timed)

  def public(i: PublicInitData): String = {
//    logger.info("Calling SSR:public ...")
    val Right((time, s)) = ctx.eval(exprPublic(i))
//    logger.info(s"        SSR:public completed in $time")
    logger.info(s"SSR:public completed in $time")
    s
  }

  // TODO Remove SsrJvm.main
  def main(args: Array[String]): Unit = {

    ctx.eval(Expr("window = {console: console, location: {protocol: 'https:', hostname: 'shipreq.com', port:'', href: 'https://shipreq.com'}, navigator: {userAgent: ''}}").void)
    ctx.eval(lib1.void)
//    ctx.eval(Expr("console.log('==============')"))
//    ctx.eval(Expr("console.log('window = ', window)"))
//    ctx.eval(Expr("console.log('==============')"))
//    ctx.eval(Expr("console.log('React = ', React)"))
//    ctx.eval(Expr("console.log('global.React = ', global.React)"))
//    ctx.eval(Expr("console.log('global['React'] = ', global['React'])"))
//    ctx.eval(Expr("console.log('window.React = ', window.React)"))
//    ctx.eval(Expr("console.log('window['React'] = ', window['React'])"))
//    ctx.eval(Expr("console.log('==============')"))
//    ctx.eval(Expr("console.log('ReactDOM = ', ReactDOM)"))
//    ctx.eval(Expr("console.log('global.ReactDOM = ', global.ReactDOM)"))
//    ctx.eval(Expr("console.log('global['ReactDOM'] = ', global['ReactDOM'])"))
//    ctx.eval(Expr("console.log('window.ReactDOM = ', window.ReactDOM)"))
//    ctx.eval(Expr("console.log('window['ReactDOM'] = ', window['ReactDOM'])"))
//    ctx.eval(Expr("console.log('==============')"))
//    ctx.eval(Expr("console.log('ReactDOMServer = ', ReactDOMServer)"))
//    ctx.eval(Expr("console.log('global.ReactDOMServer = ', global.ReactDOMServer)"))
//    ctx.eval(Expr("console.log('global['ReactDOMServer'] = ', global['ReactDOMServer'])"))
//    ctx.eval(Expr("console.log('window.ReactDOMServer = ', window.ReactDOMServer)"))
//    ctx.eval(Expr("console.log('window['ReactDOMServer'] = ', window['ReactDOMServer'])"))
//    ctx.eval(Expr("console.log('==============')"))
    ctx.eval(lib2.void)

    import shipreq.base.util.Allow
    import shipreq.webapp.base.protocol.{ServerSideProc, ServerSideProcId}
    import shipreq.webapp.client.public.PublicSpaProtocols._

    val sspId = ServerSideProcId("")

    val landingPage = ServerSideProc(sspId, LandingPage.Fn)
    val register1 = ServerSideProc(sspId, Register.Fn1)
    val register2 = ServerSideProc(sspId, Register.Fn2)
    val login = ServerSideProc(sspId, Login.Fn)
    val resetPassword1 = ServerSideProc(sspId, ResetPassword.Fn1)
    val resetPassword2 = ServerSideProc(sspId, ResetPassword.Fn2)

    val i = PublicInitData(
      publicRegistration = Allow,
      loggedInUser = None,
      landingPage = landingPage,
      register1 = register1,
      register2 = register2,
      login = login,
      resetPassword1 = resetPassword1,
      resetPassword2 = resetPassword2)

    (0 to 10).foreach(_ => public(i))
    println(public(i))
  }
}

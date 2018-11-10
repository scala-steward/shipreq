package shipreq.webapp.ssr

import japgolly.scalajs.react.{Callback, raw}
import japgolly.scalajs.react.extra.router.{BaseUrl, Router}
import scalaz.\/
import shipreq.base.util.Permission
import shipreq.webapp.base.protocol.{ClientProtocol, ServerSideProc, ServerSideProcId}
import shipreq.webapp.base.user.Username

object SsrMain extends SsrLibrary[raw.React.Element] {

  private val cp: ClientProtocol =
    new ClientProtocol {
      override def call[I, O](proc      : ServerSideProc[I, O])
                             (input     : I,
                              onResponse: Throwable \/ O => Callback): Callback =
        Callback.empty
    }

  private val sspId = ServerSideProcId("")

  // ===================================================================================================================

  override def public(publicRegistration: Permission,
                      loggedInUser      : Option[Username]): raw.React.Element = {

    import shipreq.webapp.client.public.spa._
    import shipreq.webapp.client.public.PublicSpaProtocols._

    val landingPage    = ServerSideProc(sspId, LandingPage.Fn)
    val register1      = ServerSideProc(sspId, Register.Fn1)
    val register2      = ServerSideProc(sspId, Register.Fn2)
    val login          = ServerSideProc(sspId, Login.Fn)
    val resetPassword1 = ServerSideProc(sspId, ResetPassword.Fn1)
    val resetPassword2 = ServerSideProc(sspId, ResetPassword.Fn2)

    val i = InitData(
      publicRegistration = publicRegistration,
      loggedInUser       = loggedInUser,
      landingPage        = landingPage,
      register1          = register1,
      register2          = register2,
      login              = login,
      resetPassword1     = resetPassword1,
      resetPassword2     = resetPassword2)

    val spa     = new PublicSpa(i, cp)
    val baseUrl = BaseUrl.fromWindowOrigin
    val router  = Router(baseUrl, Routes.routerConfig(spa))
    router().raw
  }
}

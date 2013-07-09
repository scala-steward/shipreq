package com.beardedlogic.usecase
package snippet

import net.liftweb.http.{S, StatefulSnippet, SHtml}
import net.liftweb.util.Helpers._
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc._

import app.AppSiteMap
import lib.SnippetHelpers
import util.Reactor
import util.HtmlTransformExt.ajaxSubmitOnClick

class Login extends StatefulSnippet with SnippetHelpers {
  override def dispatch = { case _ => render }

  private var usernameOrEmail, password = ""
  private var rememberMe = true

  // TODO What about when user already logged in?

  def render = (
    "#who" #> SHtml.onSubmit(i => usernameOrEmail = i.trim) &
      "#who [value]" #> usernameOrEmail &
      "#password" #> SHtml.onSubmit(password = _) &
      "#remember" #> SHtml.checkbox(rememberMe, rememberMe = _, "id" -> "remember") &
      ":submit" #> ajaxSubmitOnClick(jsCallback(onLoginAttempt(_)))
    )

  def onLoginAttempt(implicit reactor: Reactor) {
    // TODO Check password length when range constraints implemented
    if (usernameOrEmail.isEmpty || password.isEmpty)
      reactWithError("Invalid login details.")
    else {
      val subject = SecurityUtils.getSubject
      val loginToken = new UsernamePasswordToken(usernameOrEmail, password)
      loginToken.setRememberMe(rememberMe)
      try {
        subject.login(loginToken)
        onSuccessfulLogin()
      } catch {
        case _: AuthenticationException => reactWithError("Invalid login details.")
      }
    }
  }

  def onSuccessfulLogin() {
    // TODO update login count async
    daoProvider.withSession(_.updateUserOnLogin(loggedInUser.get.id, clientIp_Or_?))
    S.redirectTo(AppSiteMap.HomeRelativeUrl)
  }
}

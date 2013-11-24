package com.beardedlogic.shipreq.snippet

import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import com.beardedlogic.shipreq.app.AppSiteMap
import com.beardedlogic.shipreq.lib.SnippetHelpers
import com.beardedlogic.shipreq.lib.Types.UserId
import com.beardedlogic.shipreq.security.PasswordAndSalt

/**
 * Allows user to view and modify their account details.
 */
object UserAccount extends SnippetHelpers {

  def render = {
    val u = currentUser_!
    val uu = daoProvider.withSession(_ findUserSupplementalInfo u) getOrElse redirectTo(AppSiteMap.Logout)
    (
      ".username .form-control-static *" #> u.username
      & ".email .form-control-static *" #> u.email
      & ".registeredAt time [datetime]" #> uu.registeredAt
      & ".password .edit" #> DynModal.passwordChangerT("Account Password", Some(uu.ps))(onPasswordChange(u))
    )
  }

  def onPasswordChange(id: UserId)(newPassword: PasswordAndSalt): JsCmd = {
    daoProvider.withSession(_.updateUserPassword(id, newPassword))
    jsShowNotice("Updated Account Password.")
  }
}

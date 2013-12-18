package com.beardedlogic.shipreq
package snippet

import java.sql.Connection
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import org.joda.time.DateTime

import app.AppConfig.PasswordResetTokenLifespan
import db.{Dao, DaoT, UserRegistrationInfo, ResetPasswordInfo}
import feature.validation.Validator
import lib.MailHelpers.MailContent
import lib.SingleOpStatefulSnippet
import lib.Types._
import mail.PasswordResetEmails
import util.HtmlTransformExt.ajaxSubmitOnClick
import util.JsExt._
import ResetPassword._

object ResetPassword {
  def isTokenExpired(dateIssued: DateTime): Boolean = PasswordResetTokenLifespan.ago.isAfter(dateIssued)
}

class ResetPassword1 extends SingleOpStatefulSnippet {

  var emailInput = ""

  def render = (
    "#email" #> SHtml.onSubmit(emailInput = _) &
    ":submit" #> ajaxSubmitOnClick(onSubmit)
  )

  def onSubmit(): JsCmd = {
    securityProvider.enforceHumanSpeed()
    onSubmit(emailInput)
  }

  def onSubmit(emailInput: String): JsCmd = {
    val email = Validator.email.correct(emailInput)
    daoProvider.withTransaction(dao =>
      Dao.withTransactionLevel(dao, Connection.TRANSACTION_SERIALIZABLE) {
        dao.findUserRegAndResetPwInfo(email) match {

          case None =>
            ifValid(Validator.email.validate(email))(_ => jsEmailSent)

          case Some((u@UserRegistrationInfo(_, _, _, None), _)) =>
            send(email, (new Register1).performPreRegistation(u, dao))

          case Some((UserRegistrationInfo(id, _, _, Some(_)), ResetPasswordInfo(Some(token), Some(issued)))) if !isTokenExpired(issued) =>
            send(email, reuseToken(id, token, dao))

          case Some((UserRegistrationInfo(id, _, _, Some(_)), _)) =>
            send(email, issueNewToken(id, dao))
  }})}

  def send(emailAddr: String, mail: MailContent): JsCmd = {
    sendMail(mail addressedTo emailAddr)
    jsEmailSent
  }

  def jsEmailSent: JsCmd =
    jsClearError & JqExpr("#resetpw1Form,#resetpwTokenSent") ~> JqToggle

  private def issueNewToken(id: UserId, dao: DaoT): MailContent = {
    val token = dao.performInstallNewResetPasswordToken(id, () => randomConfirmationToken)
    PasswordResetEmails.PasswordChangeRequest(token)
  }

  private def reuseToken(id: UserId, token: String, dao: DaoT): MailContent = {
    dao.performReuseResetPasswordToken(id)
    PasswordResetEmails.PasswordChangeRequest(token)
  }
}

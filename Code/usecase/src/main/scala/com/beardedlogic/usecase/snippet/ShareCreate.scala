package com.beardedlogic.usecase.snippet

import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import com.beardedlogic.usecase.app.AppSiteMap
import com.beardedlogic.usecase.feature.{InputValidator, UcFilters, UcFilter}
import com.beardedlogic.usecase.lib.SingleOpStatefulSnippet
import com.beardedlogic.usecase.lib.Types.ProjectId
import com.beardedlogic.usecase.security.PasswordAndSalt
import com.beardedlogic.usecase.util.HtmlTransformExt.ajaxSubmitOnClick

/**
 * Allows a user to create a new share.
 *
 * @since 30/10/2013
 */
class ShareCreate(projectId: ProjectId) extends SingleOpStatefulSnippet {

  var nameInput = ""
  var password1Input = ""
  var password2Input = ""
  var prefaceInput = ""

  def render = {
    val ucs = daoProvider.withSession(_.findAllLatestUseCaseRevsByProject(projectId))
    val (ucFilterXml, ucFilterFn) = UcFilter.render(UcFilters.All, ucs)

    (
      "#shareName" #> SHtml.onSubmit(nameInput = _)
      & "#password1" #> SHtml.onSubmit(password1Input = _)
      & "#password2" #> SHtml.onSubmit(password2Input = _)
      & "#preface" #> SHtml.onSubmit(prefaceInput = _)
      & "#uc-filters" #> ucFilterXml
      & ":submit" #> ajaxSubmitOnClick(() => onSubmit(ucFilterFn))
    )
  }

  def onSubmit(ucFilterFn: () => UcFilter): JsCmd = try {
    val nameV = InputValidator.shareName.correctAndValidate(nameInput)
    val passwordsV = InputValidator.passwords.correctAndValidate(password1Input, password2Input)
    val prefaceV = InputValidator.sharePreface.correctAndValidate(prefaceInput)

    val possibleJs = for {
      name     <- nameV
      password <- passwordsV
      prefaceT <- prefaceV
    } yield {
      val ucFilter = ucFilterFn()
      val ps = PasswordAndSalt.createWithRandomSalt(password)
      val preface = nonEmptyString(prefaceT)
      val ucFilterJson = UcFilter.toJson(ucFilter)

      // TODO notice and/or make it show shares tab
      daoProvider.withSession(_.createShare(projectId, ps, name, preface, ucFilterJson))
      redirectTo(AppSiteMap.Project)(projectId)
    }

    possibleJs | jsShowErrors(collectErrors(List(nameV, passwordsV, prefaceV)))
  } finally {
    password1Input = "" // Let's not keep the plaintext passwords around
    password2Input = ""
  }
}

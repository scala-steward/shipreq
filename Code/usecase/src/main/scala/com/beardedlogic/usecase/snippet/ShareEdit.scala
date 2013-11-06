package com.beardedlogic.usecase.snippet

import com.beardedlogic.usecase.app.{RequestVars, AppSiteMap}
import com.beardedlogic.usecase.feature.validation.Validator
import com.beardedlogic.usecase.feature.UcFilter
import com.beardedlogic.usecase.lib.{NoticeFlash, SingleOpStatefulSnippet}
import com.beardedlogic.usecase.util.HtmlTransformExt.{IfCssSel, ajaxSubmitOnClick}
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import project.ActivateTab

/**
 * Allows a user to edit a new share.
 *
 * @since 6/11/2013
 */
class ShareEdit extends SingleOpStatefulSnippet {

  val share = RequestVars.Share.value
  @inline def projectId = share.projectId

  var nameInput = ""
  var prefaceInput = ""

  def render = {
    val ucs = daoProvider.withSession(_.findAllLatestUseCaseRevsByProject(projectId))
    val (ucFilterXml, ucFilterFn) = UcFilter.render(share.ucFilter, ucs)

    (
      "#shareName" #> SHtml.onSubmit(nameInput = _)
      & "#preface" #> SHtml.onSubmit(prefaceInput = _)
      & "#uc-filters" #> ucFilterXml
      & "#shareName [value]" #> share.name
      & IfCssSel(share.preface.isDefined)("#preface *" #> share.preface.get)
      & ":submit" #> ajaxSubmitOnClick(() => onSubmit(ucFilterFn))
    )
  }

  def onSubmit(ucFilterFn: () => UcFilter): JsCmd = {
    val v = Validator.Ap.apply2(
      Validator.shareName.correctAndValidate(nameInput),
      Validator.sharePreface.correctAndValidate(prefaceInput)
    )(Tuple2.apply)

    ifValid(v)(r => {
      val (name, prefaceT) = r
      val ucFilter = ucFilterFn()
      val preface = nonEmptyString(prefaceT)
      val ucFilterJson = UcFilter.toJson(ucFilter)
      daoProvider.withSession(_.updateShare(share, name, preface, ucFilterJson))
      postCreation()
    })
  }

  def postCreation(): Nothing = {
    NoticeFlash.notices.addS("Share updated successfully.")
    ActivateTab.SharesTab.setInFlash()
    redirectTo(AppSiteMap.Project)(projectId)
  }
}

package com.beardedlogic.shipreq.snippet

import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import scala.xml.NodeSeq
import com.beardedlogic.shipreq.app.RequestVars
import com.beardedlogic.shipreq.feature.UcFilter
import com.beardedlogic.shipreq.feature.validation.Validator
import com.beardedlogic.shipreq.lib.NoticeFlash
import com.beardedlogic.shipreq.lib.Types.Json
import com.beardedlogic.shipreq.util.HtmlTransformExt.IfCssSel
import com.beardedlogic.shipreq.util.NonEmptyTemplate

object ShareEditConsts {

  val EditForm: NodeSeq = {
    val createForm = NonEmptyTemplate.load("loggedin/share-create").extract("form")
                     .assertHeadType("form").assertSingleHead.get
    val transform = (
      ".password .form-group" #> "" &
      ":submit *" #> "Update Share"
    )
    transform(createForm)
  }
}

/**
 * Allows a user to edit a new share.
 *
 * @since 6/11/2013
 */
class ShareEdit extends ShareCreateBase {

  val share = RequestVars.Share.value
  def projectId = share.projectId

  def render =
    "#edit-form" #> ShareEditConsts.EditForm andThen (
      render2(share.ucFilter)
        & "#shareName [value]" #> share.name
        & IfCssSel(share.preface.isDefined)("#preface *" #> share.preface.get)
      )

  def onSubmit(ucFilterJson: () => Json[UcFilter]): JsCmd = {
    val v = Validator.Ap.apply2(nameV, prefaceV)(Tuple2.apply)
    ifValid(v)(r => {
      val (name, preface) = r
      daoProvider.withSession(_.updateShare(share, name, preface, ucFilterJson()))
      NoticeFlash.notices.addS("Share updated successfully.")
      goBackToShareList()
    })
  }
}

package shipreq.webapp.client.project.app.pages.admin.status

import shipreq.webapp.base.test.TestState._
import shipreq.webapp.client.project.test._
import shipreq.webapp.member.test.CommonObs

final class StatusPageObs($: DomZipperJs, val global: TestGlobal.Obs) {

  val deleteButton  = $.collect01(".ui.button.negative").map(new CommonObs.SemanticUiButton(_))
  val cancelButton  = $.collect01(".ui.button.black").map(new CommonObs.SemanticUiButton(_))
  val restoreButton = $.collect01(".ui.button.green").map(new CommonObs.SemanticUiButton(_))

  val message  = $.collect01(".ui.message").zippers
  val table    = $.collect01("table").zippers
  val textarea = $.collect01("textarea").zippers
}

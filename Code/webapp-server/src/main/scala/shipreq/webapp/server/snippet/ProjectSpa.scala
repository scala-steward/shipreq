package shipreq.webapp.server.snippet

import net.liftweb.util.Helpers._
import shipreq.base.util.FxModule._
import shipreq.webapp.base.AssetManifest
import shipreq.webapp.base.protocol.entrypoint.ProjectSpaEntryPoint
import shipreq.webapp.server.app.{Global, LiftDispatcher}
import shipreq.webapp.server.protocol.entrypoint.{ClientSideProcInvoker, LoadJs}
import shipreq.webapp.ssr.Html
import shipreq.webapp.ssr.SsrSharedData.ProjectSpaLoaderData

object ProjectSpa extends SingleOpStatelessSnippet {

  private def ResourceBundle = {
    val am = new AssetManifest
    val sjsm = Global.config.server.scalaJsManifest
    LoadJs.Bundle(
      LoadJs.Resource(am.semanticJs),
      LoadJs.Resource(am.reactJs),
      LoadJs.Resource(am.reactDomJs),
      LoadJs.Resource(am.reactDomServerJs),
      LoadJs.Resource(am.memberLibBundleJs),
      LoadJs.Resource(sjsm.project),
      LoadJs.Resource(am.katexCss),
      LoadJs.Resource(am.katexJs),
      LoadJs.Resource(am.prismJsCss),
      LoadJs.Resource(am.prismJsCore),
      LoadJs.Resource(am.prismJsAutoloader),
      LoadJs.Resource(am.prismJsLineNumbers),
      LoadJs.Resource(am.prismJsLineNumbersCss),
      LoadJs.Resource(am.prismJsMatchBraces),
      LoadJs.Resource(am.prismJsMatchBracesCss),
    )
  }

  val EntryPoint = ClientSideProcInvoker(ProjectSpaEntryPoint.proc, ResourceBundle)

  private[this] val ssrFallback = Html(
    """<div style="margin-top:33vh;text-align:center;font-size:150%;color:#333;">loading ...</div>""")

  override def render = {
    val projectId = LiftDispatcher.ProjectIdVar.is
    assert(projectId != null, "Project SPA snippet invoked without a ProjectId")

    val user = currentUser_!()

    val logic = Global.logic.projectSpa

    val init: ProjectSpaEntryPoint.InitData =
      logic.initPage(projectId, user.username).unsafeRun()

    val loaderData =
      ProjectSpaLoaderData(user.username, init.projectName)

    val loaderHtml =
      Global.ssr.projectSpaLoader(loaderData).unsafeRun()
        .getOrElse(ssrFallback)
        .xml

    "*" #> (loaderHtml :+ EntryPoint.invokeOnLoadHtml(init))
  }
}
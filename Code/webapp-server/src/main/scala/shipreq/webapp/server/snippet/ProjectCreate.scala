package shipreq.webapp.server.snippet

import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import shipreq.webapp.server.app.AppSiteMap
import shipreq.webapp.server.db.CreateProjectResult._
import shipreq.webapp.server.feature.validation.Validators
import shipreq.webapp.server.lib.{FormVar, SingleOpStatefulSnippet}
import shipreq.webapp.server.util.HtmlTransformExt.ajaxSubmitOnClick

/**
 * Form to create a new project.
 *
 * @since 24/09/2013
 */
object ProjectCreate extends SingleOpStatefulSnippet {

  val form = FormVar.strOnSubmit(Validators.project.name, ":text")

  def render = {
    var vars: form.Var = ""
    form.csssel(vars, vars = _) &
      ":submit" #> ajaxSubmitOnClick(() => onSubmit(vars))
  }

  def onSubmit(vars: form.Var): JsCmd =
    ifValid(form validate vars)(name =>
      daoProvider.withSession(_.createProject(currentUserId_!, name)) match {
        case DbSuccess(id)    => redirectTo(AppSiteMap.Project)(id)
        case NameAlreadyInUse => jsShowError("You already have a project with that name.")
      }
    )
}

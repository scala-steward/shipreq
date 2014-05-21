package shipreq.webapp.snippet

import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import shipreq.webapp.app.AppSiteMap
import shipreq.webapp.db.CreateProjectResult._
import shipreq.webapp.feature.validation.Validators
import shipreq.webapp.lib.{FormVar, SingleOpStatefulSnippet}
import shipreq.webapp.util.HtmlTransformExt.ajaxSubmitOnClick

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

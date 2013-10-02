package com.beardedlogic.usecase
package snippet.project

import net.liftweb.http.js.JsCmd
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._

import db.UpdateProjectResult
import lib.SingleOpStatefulSnippet
import lib.Types._
import util.HtmlTransformExt.ajaxSubmitOnClick
import util.JsExt.JsTextTrigger
import lib.security.PermissionCheck

private[project] object HeaderConsts {
  final val TriggerProjectUpdated = JsTextTrigger("project-updated")
}

/**
 * Renders the header on the project page.
 *
 * @since 30/09/2013
 */
class Header(projectId: ProjectId) extends SingleOpStatefulSnippet {
  import HeaderConsts._

  val project = requireResultO_!(daoProvider.withSession(_.findProject(projectId)))
  PermissionCheck.userCan readAndUpdate project andIfNotThen redirectHome

  private[snippet] var projectName = project.name

  implicit def alertId = "phdra".tag[AlertIdTag]

  def render = (
    "#project-title" #> (
      "h1 *" #> project.name &
      "input .title [value]" #> project.name &
      "input .title" #> SHtml.onSubmit(projectName = _) &
      "button .update" #> ajaxSubmitOnClick(onRename)
    )
  )

  def onRename(): JsCmd = {
    import UpdateProjectResult._
    daoProvider.withSession(_.updateProject(projectId, currentUserId_!, projectName)) match {
      case Success(newName) => jsRenamed(newName)
      case InvalidName      => jsShowError("Invalid project name.")
      case NameAlreadyInUse => jsShowError("You already have a project with that name.")
      case ProjectNotFound  => redirectHome
    }
  }

  def jsRenamed(newName: String): JsCmd = (
    jsClearError
    & jsShowAlertSuccess("Project renamed successfully.")
    & TriggerProjectUpdated.trigger(newName)
  )
}

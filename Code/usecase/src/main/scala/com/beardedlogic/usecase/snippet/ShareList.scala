package com.beardedlogic.usecase
package snippet

import net.liftweb.util.Helpers._
import app.AppSiteMap
import AppSiteMap.Implicits._
import lib.Types.ProjectId

/**
 * Displays a list of a user's shares.
 *
 * @since 30/10/2013
 */
class ShareList(projectId: ProjectId) {

  def render =
    ".create a [href]" #> AppSiteMap.ShareCreate.relativeUrl(projectId)
}

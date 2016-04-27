package shipreq.webapp.server
package snippet

import net.liftweb.util.Helpers._
import app.AppSiteMap
import lib.SnippetHelpers
import AppSiteMap.Implicits._
import shipreq.webapp.server.lib.Types.ISO8601

/**
 * Displays a list of a user's projects.
 *
 * @since 27/09/2013
 */
object ProjectList extends SnippetHelpers {

  def render =
    "ol" #> "" // TODO Delete
}

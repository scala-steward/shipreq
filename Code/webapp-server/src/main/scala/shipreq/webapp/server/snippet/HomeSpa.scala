package shipreq.webapp.server.snippet

import net.liftweb.util.Helpers._
import shipreq.webapp.base.protocol.InitDataForHomeSpa
import shipreq.webapp.server.lib.SnippetHelpers
import shipreq.webapp.server.protocol.ClientFn

object HomeSpa extends SnippetHelpers {

  def render = {
    val user = currentUser_!()
    val projects = daoProvider.withSession(_.getProjectCatalogue(user.id))
    val data = InitDataForHomeSpa(user.username, projects)
    "*" #> ClientFn.HomeSpa.runOnLoadHtml(data)
  }
}

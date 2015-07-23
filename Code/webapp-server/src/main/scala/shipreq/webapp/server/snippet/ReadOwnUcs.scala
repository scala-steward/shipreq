package shipreq.webapp.server.snippet

import net.liftweb.util.Helpers._
import net.liftweb.util.CssSel
import shipreq.webapp.server.app.{AppSiteMap, RequestVars, DI}
import shipreq.webapp.server.feature.uc.persist.UseCasePersistence
import shipreq.webapp.server.lib.Locks
import shipreq.webapp.server.lib.Types._
import shipreq.webapp.server.feature.publish.{Input, HtmlPublisher}
import AppSiteMap.Implicits._

object ReadOwnUcs extends DI {

  def render: CssSel = {
    val project = RequestVars.Project.get.value

    val ucs =
      daoProvider.withTransaction(dao =>
        Locks.UseCaseNumbers.read(project)(lock =>
          UseCasePersistence.loadAll(project).run(dao, lock)))
      .map(_.ucAndRev)

    if (ucs.isEmpty)
      "a [href]" #> AppSiteMap.Project.relativeUrl(project)
    else {
      val i = new Input(None, ucs)
      val o = HtmlPublisher.publish(i)
      "* *" #> o
    }
  }
}

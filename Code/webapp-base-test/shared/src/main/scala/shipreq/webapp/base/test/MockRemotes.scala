package shipreq.webapp.base.test

import java.time.Instant
import java.time.temporal.ChronoUnit
import shipreq.webapp.base.data.{ExternalId, Project, ProjectCatalogue, Username}
import shipreq.webapp.base.protocol._

object MockRemotes {

  lazy val createProjectFn = RemoteFn.Instance("CreateProject", CreateProjectFn)

  def mockUsername = Username("testuser")

  def projectSpa(p: Project): InitDataForProjectSpa =
    projectSpa(p, mockUsername)

  def projectSpa(p: Project, username: Username): InitDataForProjectSpa = {
    val now = Instant.now()
    val pi = ProjectCatalogue.Item(
      ExternalId("test"),
      p.name,
      1000,
      p.reqs.size,
      now.minus(99, ChronoUnit.DAYS),
      Some(now.minus(32, ChronoUnit.HOURS)))
    projectSpa(pi, username)
  }

  def projectSpa(p: ProjectCatalogue.Item): InitDataForProjectSpa =
    projectSpa(p, mockUsername)

  def projectSpa(p: ProjectCatalogue.Item, username: Username): InitDataForProjectSpa =
    InitDataForProjectSpa(
      username,
      p,
      RemoteFn.Instance("projectInit"   , ProjectInit          ),
      RemoteFn.Instance("issueTypeCrud" , CustomIssueTypeCrud  ),
      RemoteFn.Instance("reqTypeCrud"   , CustomReqTypeCrud    ),
      RemoteFn.Instance("reqTypeImpMod" , ReqTypeImplicationMod),
      RemoteFn.Instance("fieldMandMod"  , FieldMandatorinessMod),
      RemoteFn.Instance("fieldCrud"     , FieldCrud.Fn         ),
      RemoteFn.Instance("tagCrud"       , TagCrud.Fn           ),
      RemoteFn.Instance("createContent" , CreateContentFn      ),
      RemoteFn.Instance("updateContent" , UpdateContentFn      ),
      RemoteFn.Instance("projectNameSet", ProjectNameSetFn     ))
}

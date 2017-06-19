package shipreq.webapp.base.data

import nyaya.prop._
import shipreq.base.test.BaseTestUtil._

case class ProjectMetaDataProps(md: ProjectMetaData, project: Project, eventCount: Int) {
  def assert(): Unit =
    ProjectMetaDataProps.All assert this
}

object ProjectMetaDataProps {

  type P = ProjectMetaDataProps

  val ProjectName = Prop.equal[P]("Project name")(_.project.name, _.md.name)

  val ReqCount = Prop.equal[P]("Req count")(_.project.reqs.size, _.md.reqCount)

  val EventCount = Prop.equal[P]("Event count")(_.eventCount, _.md.eventCount)

  val All: Prop[P] =
    ProjectName & ReqCount & EventCount
}

package shipreq.webapp.member.event

import shipreq.webapp.member.event.ApplyEventTestFns._
import shipreq.webapp.member.event.Event._
import shipreq.webapp.member.event.NoInitialEvents._
import utest._

object TemplateTest extends TestSuite {

  override def tests = Tests {

    // All templates must apply to a new project
    for (t <- ProjectTemplate.values)
      assertPass(ProjectTemplateApply(t))
  }
}

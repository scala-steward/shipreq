package com.beardedlogic.shipreq.snippet.project

import org.mockito.Mockito.when
import org.scalatest.FunSuite
import scalaz.Value
import com.beardedlogic.shipreq.app.RequestVars
import com.beardedlogic.shipreq.db.{Project, UserDescriptor, UpdateProjectResult}
import com.beardedlogic.shipreq.test.{MockDaoProvider, TestHelpers}
import com.beardedlogic.shipreq.lib.Types._
import com.beardedlogic.shipreq.util.NonEmptyTemplate
import UpdateProjectResult._
import ProjectHeaderConsts._

class ProjectHeaderTest extends FunSuite with TestHelpers {

  lazy val html = NonEmptyTemplate.load("loggedin/project").get

  def run[R](
    loggedInUser: Option[UserDescriptor] = Some(UD1),
    project: Project = Project(123456.tag[IsProjectId], "Grrr", UD1.id),
    updateResult: UpdateProjectResult = DbSuccess
    )(fn: ProjectHeader => R = identity[ProjectHeader] _): R = {

    val uid: UserId = loggedInUser.map(_.id).getOrElse((-1).tag[IsUserId])
    MockDaoProvider(dao => {
      when(dao.updateProject(meq(project.id), meq(uid), any)).thenReturn(updateResult)
    }).install {
      withUserLoggedIn(loggedInUser) {
        inMockSession {
          RequestVars.Project.set(Value(project))
          val h = new ProjectHeader
          fn(h)
        }
      }
    }
  }

  test("Render with valid project & user") {
    val output = run()(_.render(html))
    output.toString should include("Grrr")
  }

  test("Successful rename") {
    val js = run()(p => {p.projectNameInput = "YAY"; p.onRename})
    js.toString should (include("YAY") and include(TriggerProjectUpdated.triggerName))
  }
}

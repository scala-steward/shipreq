package shipreq.webapp.server.app

import utest._
import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.base.{AssetManifest, MemberUrls, WebappConfig}
import shipreq.webapp.base.protocol._
import shipreq.webapp.client.public.PublicSpaProtocols
import shipreq.webapp.server.logic.ProjectId
import shipreq.webapp.server.test.LiveTestUtils._
import shipreq.webapp.server.test._

object LiveTest extends TestSuite {

  import userFixture.{user1, user2}

  var pid = Option.empty[ProjectId]

  val prepare = once {
    LiveTestUtils.init()
    userFixture.setup.unsafePerformIO()
    pid = Some(xa ! dbAlgebra.createEmptyProject(user1.id))
  }

  override def tests = TestSuite {
    prepare()

    'root {
      get("/")
        .assertSpa(AssetManifest.webappClientPublicJs, PublicSpaProtocols.EntryPoint)
        .assertBodyTitle(WebappConfig.makePageTitle())
      ()
    }

    'liftAjax {
      val root = get("/")
      val ajaxUrl = s"/${WebappConfig.liftPath}/[a-zA-Z0-9_/]+\\.js".r.findFirstIn(root.bodyString) getOrElse fail(s"Lift Ajax not found in: ${root.bodyString}")
      get(ajaxUrl, headers = retainSession(root))
        .assertOk
        .assertContentTypeJs
        .assertBodyContains("lift_settings")
      ()
    }

    'webappClientPublicJs {
      get(AssetManifest.webappClientPublicJs)
        .assertOk
        .assertContentTypeJs
        .assertBodyContains("function")
        .assertBodyContains("public")
      ()
    }

    'favicon {
      get(AssetManifest.favicon)
        .assertOk
        .assertContentType("image/x-icon")
      ()
    }

    'logout - {
      get(MemberUrls.logout.relativeUrl)
        .assertRedirectTo("/")
      ()
    }

    'membersHome {
      get(MemberUrls.home.relativeUrl, headers = retainSession(login(user1)))
        .assertSpa(AssetManifest.webappClientHomeJs, HomeSpaProtocols.EntryPoint)
        .assertBodyTitle(WebappConfig.makePageTitle())
      ()
    }

    'projectSpa {
      val p = ProjectId.Extern(pid.get)
      get(MemberUrls.project(p).relativeUrl, headers = retainSession(login(user1)))
        .assertSpa(AssetManifest.webappClientProjectJs, ProjectSpaProtocols.EntryPoint)
      ()
    }

    // ensure we don't block these (and other Lift stuff we don't know about)
    'contentSecurityPolicyReport {
      get(s"/${WebappConfig.liftPath}/content-security-policy-report")
        .assertBodyContains("content security policy report")
      ()
    }

    'teardown {
      xa ! DbTable.EventHash.truncate
      xa ! DbTable.Event.truncate
      xa ! DbTable.Project.truncate
      userFixture.teardown.unsafePerformIO()
    }
  }
}

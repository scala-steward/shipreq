package shipreq.webapp.server.app

import utest._
import shipreq.webapp.base.{AssetManifest, MemberUrls, WebappConfig}
import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.server.test.LiveTestUtils._

object LiveTest extends TestSuite {

  override def tests = TestSuite {
    'root {
      get("/")
        .assertOk
        .assertContentTypeHtml
        .assertBodyContains(AssetManifest.webappClientPublicJs)
      ()
    }

    'liftAjax {
      val root = get("/")
      val ajaxUrl = s"/${WebappConfig.liftPath}/[a-zA-Z0-9_/]+\\.js".r.findFirstIn(root.bodyString) getOrElse fail(s"Lift Ajax not found in: ${root.bodyString}")
      val cookie = root.headers.get("Set-Cookie")
      get(ajaxUrl, headers = cookie.toList.flatten.map(("Cookie", _)))
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
  }
}

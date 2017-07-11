package shipreq.webapp.client.public.spa

import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router.{RouterCtl => _, _}
import japgolly.scalajs.react.vdom.Implicits._
import shipreq.base.util.univeq._
import shipreq.webapp.base.Urls.PublicSpaRoute
import shipreq.webapp.base.WebappConfig
import shipreq.webapp.base.lib.BaseReusability._

sealed trait Page {
  val route: PublicSpaRoute
  val linkTitle: String
  val pageTitle: List[String]
}
object Page {

  final case class Static(route: PublicSpaRoute.Static) extends Page {
    override val linkTitle: String =
      route match {
        case PublicSpaRoute.Home           => "Home"
        case PublicSpaRoute.Login          => "Login"
        case PublicSpaRoute.Privacy        => "Privacy"
        case PublicSpaRoute.Register1      => "Register"
        case PublicSpaRoute.TermsOfService => "Terms"
      }

    val pageTitle: List[String] =
      route match {
        case PublicSpaRoute.Home => Nil
        case _                   => linkTitle :: Nil
      }
  }

  implicit def equality: UnivEq[Page] = UnivEq.derive
  implicit def reusability: Reusability[Page] = Reusability.byUnivEq
  val static = PublicSpaRoute.static.map(Static)

  val Home           = Static(PublicSpaRoute.Home)
  val Login          = Static(PublicSpaRoute.Login)
  val Privacy        = Static(PublicSpaRoute.Privacy)
  val Register1      = Static(PublicSpaRoute.Register1)
  val TermsOfService = Static(PublicSpaRoute.TermsOfService)
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object Routes {

  def routerConfig(spa: PublicSpa) =
    RouterConfigDsl[Page].buildConfig { dsl =>
      import dsl._

      def render(page: Page, r: RouterCtl) =
        spa.Component(PublicSpa.Props(page, r))

//      val dynPage = dynRenderR((page: Page, r) => render(page, r))

      def staticPage(route: StaticDsl.Route[Unit], page: Page) =
        staticRoute(route, page) ~> renderR(r => render(page, r))

      val staticRoutes =
        Page.static.map { p =>
          val url = p.route.url
          staticPage(if (url.isRoot) dsl.root else url.relativeUrl, p)
        }.reduce(_ | _)

      (staticRoutes | trimSlashes)
        .notFound(redirectToPage(Page.Home)(Redirect.Replace))
        .setTitle(p => WebappConfig.makePageTitle(p.pageTitle: _*))
        .verify(Page.static.head, Page.static.tail: _*)
    }
}

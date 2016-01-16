package shipreq.webapp.client.app

import japgolly.scalajs.react._, vdom.prefix_<^._, MonocleReact._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.extra.router.{RouterCtl => RouterCtl_, _}
import monocle.macros.Lenses
import org.scalajs.dom
import scalacss.Defaults._
import scalacss.ScalaCssReact._

import shipreq.base.util.{NonEmptyVector, UnivEq}
import shipreq.webapp.base.protocol.ProjectSPA
import shipreq.webapp.client.app.cfg.shared.Usage
import shipreq.webapp.client.app.state.ClientData
import shipreq.webapp.client.data.{FilterDead, HideDead}
import shipreq.webapp.client.protocol.ClientProtocol

object ProjectSpaMain {

  def main(remotes: ProjectSPA): Callback = {
    val cp = ClientProtocol.Default
    ClientData.init(cp, remotes.projectInit, cd => Callback {
      Style.addToDocument()
      val main    = new ProjectSpaMain(remotes, cp, cd)
      val baseUrl = determineBaseUrl(dom.window.location.href)
      val router  = Router(baseUrl, main.routerConfig)
      router() render dom.document.getElementById("tgt")
    })
  }

  // ===================================================================================================================
  // Routes

  type RouterCtl = RouterCtl_[Page]

  sealed trait Page
  object Page {
    case object Index       extends Page
    case object CfgFields   extends Page
    case object CfgIssues   extends Page
    case object CfgReqTypes extends Page
    case object CfgTags     extends Page
    case object ReqTable    extends Page
  }

  implicit def pageEq: UnivEq[Page] = UnivEq.derive

  val pages = {
    import Page._
    NonEmptyVector[Page](
      Index, ReqTable, CfgFields, CfgIssues, CfgReqTypes, CfgTags)
  }

  def determineBaseUrl(url: String) = {
    val pat = "^([^/#?]+//[^/#?]+/[^/#?]+/[^/#?]+)(?:[/#?].*|$)".r.pattern
    val m = pat.matcher(url)
    if (m.matches) BaseUrl(m group 1) else BaseUrl(url).endWith_/
  }

  // ===================================================================================================================
  // UI

  val IndexComponent = ReactComponentB[RouterCtl]("Index")
    .render_P(ctl =>
      <.ul(
        pages.whole.map(p =>
          <.li(ctl.link(p)(p.toString))))
    ).build
}


// =====================================================================================================================
import ProjectSpaMain._

final class ProjectSpaMain(r: ProjectSPA, cp: ClientProtocol, cd: ClientData) {

  def routerConfig =
    RouterConfigDsl[Page].buildConfig { dsl =>
      import dsl._
      import Page._

      def staticPage(route: StaticDsl.Route[Unit], page: Page) =
        staticRoute(route, page) ~> renderR(r => Component(Props(page, r)))

      ( staticPage(root,            Index      )
      | staticPage("/table",        ReqTable   )
      | staticPage("/cfg/fields",   CfgFields  )
      | staticPage("/cfg/issues",   CfgIssues  )
      | staticPage("/cfg/reqtypes", CfgReqTypes)
      | staticPage("/cfg/tags",     CfgTags    )
      | trimSlashes
      ).notFound(redirectToPage(Index)(Redirect.Replace))
        .verify(Index, pages.whole: _*)
    }

  case class Props(page: Page, routerCtl: RouterCtl)

  @Lenses
  case class State(filterDead: FilterDead, reqTable: reqtable.ReqTable.State)

  def initState = State(HideDead, reqtable.ReqTable.State.init(cd, HideDead, None))

  class Backend($: BackendScope[Props, State]) {
    val setFilterDead = ReusableFn($ zoomL State.filterDead).setState

    val reqTable = new reqtable.ReqTable(cd, cp, r.createContent, r.updateContent, $ zoomL State.reqTable)

    val usageShow =
      Usage.Show((fd, fs) =>
        $.props.runNow().routerCtl
          .onSet($.modState(State.reqTable.modify(_.setFilterDead(fd).setFilterSpec(fs()))) >> _)
          .link(Page.ReqTable))

    def render(p: Props, s: State): ReactElement = {
      def fd = ReusableVar(s.filterDead)(setFilterDead)

      def layout(content: ReactElement) =
        <.div(
          <.div(
            ^.textAlign.right,
            ^.paddingRight := "0.6ex",
            ^.marginTop := "-14px",
            p.routerCtl.link(Page.Index)("← Back")),
          content)

      p.page match {
        case Page.Index       => IndexComponent(p.routerCtl)
        case Page.CfgFields   => layout(cfg.fields.CfgFields.Props(cp, r.fieldCrud, cd, fd).component)
        case Page.CfgIssues   => layout(cfg.issues.CfgIssues.Props(cp, r.issueTypeCrud, r.reqTypeImpMod, r.fieldMandMod, cd, fd, usageShow).component)
        case Page.CfgReqTypes => layout(cfg.reqtypes.CfgReqTypes.Props(cp, r.reqTypeCrud, cd, fd, usageShow).component)
        case Page.CfgTags     => layout(cfg.tags.CfgTags.Props(cp, r.tagCrud, cd, fd).component)
        case Page.ReqTable    => layout(reqTable.Component(s.reqTable))
      }
    }
  }

  val Component = ReactComponentB[Props]("")
    .initialState(initState)
    .renderBackend[Backend]
    .build
}

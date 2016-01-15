package shipreq.webapp.client.app

import japgolly.scalajs.react._, vdom.prefix_<^._, MonocleReact._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.extra.router.{RouterCtl => RouterCtl_, _}
import monocle.macros.Lenses
import org.scalajs.dom
import scalacss.Defaults._
import scalacss.ScalaCssReact._

import shipreq.base.util.{NonEmptyVector, UnivEq}
import shipreq.webapp.base.filter.FilterSpec
import shipreq.webapp.base.protocol.ProjectSPA
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

  /**
   * This is used so that "Usage" columns in config screens (within this SPA) can have links that initialise the
   * ReqTable to a given state.
   *
   * It is cleared after a single use.
   *
   * Being a global variable, this is a shithouse solution and will be replaced eventually.
   */
  case class ReqTableNextState(fd: FilterDead, fs: Option[FilterSpec]) {
    def set: Callback =
      Callback(_reqTableNextState = Some(this))
  }

  private var _reqTableNextState: Option[ReqTableNextState] = None

  private def reqTableNextState(): ReqTableNextState = {
    val s = _reqTableNextState getOrElse ReqTableNextState(HideDead, None)
    _reqTableNextState = None
    s
  }

  // ===================================================================================================================
  // Routes

  type RouterCtl = RouterCtl_[Page]

  sealed trait Page
  case object Index       extends Page
  case object CfgFields   extends Page
  case object CfgIssues   extends Page
  case object CfgReqTypes extends Page
  case object CfgTags     extends Page
  case object ReqTable    extends Page

  implicit def pageEq: UnivEq[Page] = UnivEq.derive

  val pages = NonEmptyVector[Page](
    Index, ReqTable, CfgFields, CfgIssues, CfgReqTypes, CfgTags)

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
  case class State(filterDead: FilterDead)

  def initState = State(HideDead)

  class Backend($: BackendScope[Props, State]) {
    val setFilterDead = ReusableFn($ zoomL State.filterDead).setState

    def render(p: Props, s: State): ReactElement = {
      def ctl = p.routerCtl
      def fd = ReusableVar(s.filterDead)(setFilterDead)

      def layout(content: ReactElement) =
        <.div(
          <.div(
            ^.textAlign.right,
            ^.paddingRight := "0.6ex",
            ^.marginTop := "-14px",
            ctl.link(Index)("← Back")),
          content)

      def reqTable = {
        val s = reqTableNextState()
        reqtable.ReqTable.Props(cd, cp, r.createContent, r.updateContent, s.fd, s.fs).component
      }

      p.page match {
        case Index       => IndexComponent(p.routerCtl)
        case CfgFields   => layout(cfg.fields.CfgFields.Props(cp, r.fieldCrud, cd, fd).component)
        case CfgIssues   => layout(cfg.issues.CfgIssues.Props(cp, r.issueTypeCrud, r.reqTypeImpMod, r.fieldMandMod, cd, fd, ctl).component)
        case CfgReqTypes => layout(cfg.reqtypes.CfgReqTypes.Props(cp, r.reqTypeCrud, cd, fd, ctl).component)
        case CfgTags     => layout(cfg.tags.CfgTags.Props(cp, r.tagCrud, cd, fd).component)
        case ReqTable    => layout(reqTable)
      }
    }
  }

  val Component = ReactComponentB[Props]("")
    .initialState(initState)
    .renderBackend[Backend]
    .build
}

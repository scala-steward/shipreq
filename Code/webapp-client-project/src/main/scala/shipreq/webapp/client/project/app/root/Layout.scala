package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Resolution
import japgolly.scalajs.react.vdom.prefix_<^._
import shipreq.webapp.base.data.{ProjectCatalogue, Username}
import shipreq.webapp.base.text.PlainText
import shipreq.webapp.client.base.ui._
import shipreq.webapp.client.base.ui.semantic.Breadcrumb
import Routes.{Page, RouterCtl}

object Layout {

  final case class Props(username: Username,
                         project : ProjectCatalogue.Item,
                         rc      : RouterCtl,
                         res     : Resolution[Page]) {
    @inline def page = res.page
    @inline def render = Component(this)
  }

  def breadcrumb(page: Page, project: ProjectCatalogue.Item, rc: RouterCtl): Breadcrumb.Items = {
    def index = Breadcrumb.Item.Link(rc.link(Page.Index)(project.name))

    val tail = page match {
      case Page.Index =>
        Breadcrumb.Item.Div(project.name) :: Nil

      case Page.ReqDetail(id) =>
        val menuItems  = ProjectIndex.dropdownItems(None, rc)
        val reqTable   = rc.link(Page.ReqTable)(ProjectIndex.Item.ReqTable.title)
        val menu       = Breadcrumb.Item.LinkAndDropdown(reqTable, menuItems)
        val req        = Breadcrumb.Item.Div(PlainText pubid id, Breadcrumb.ItemState.Active)
        index :: menu :: req :: Nil

      case p =>
        val activeItem = ProjectIndex.Item.ToPage.reverse getOption p getOrElse sys.error("No breadcrumb menu item for " + p)
        val menuItems  = ProjectIndex.dropdownItems(Some(activeItem), rc)
        val menu       = Breadcrumb.Item.DropDown(activeItem.title, menuItems)
        index :: menu :: Nil
    }

    MemberNavBar.MemberHome :: tail
  }

  def render(p: Props): ReactElement =
    <.div(
      MemberNavBar.Props(p.username, breadcrumb(p.page, p.project, p.rc), Nil).render,
      p.res.render())

  val Component = FunctionalComponent(render)
}

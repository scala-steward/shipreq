package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.univeq._
import Routes.{Page, RouterCtl}
import shipreq.base.util.{Intersection, NonEmptyVector}
import shipreq.webapp.client.base.ui.semantic.{Colour, Dropdown, Icon}

object ProjectIndex {

  sealed abstract class Item(final val title   : String,
                             final val icon    : Icon,
                             final val subtitle: String)
  object Item {
    case object ReqTable    extends Item("Req Table"        , Icon.Cubes         , "View and edit all reqs.")
    case object ReqDetail   extends Item("Req Lookup"       , Icon.Cube          , "View and edit a single req.")
    case object ImpGraph    extends Item("Implication Graph", Icon.ShareAlternate, "TODO")
    case object CfgFields   extends Item("Fields"           , Icon.ListLayout    , "TODO")
    case object CfgIssues   extends Item("Issues"           , Icon.WarningSign   , "TODO")
    case object CfgReqTypes extends Item("Req Types"        , Icon.Inbox         , "TODO")
    case object CfgTags     extends Item("Tags"             , Icon.Tags          , "TODO")

    implicit def univEq: UnivEq[Item] = UnivEq.derive

    val ToPage: Intersection[Item, Page] =
      Intersection[Item, Page] {
        case ReqTable     => Some(Page.ReqTable)
        case ReqDetail    => None
        case ImpGraph     => Some(Page.ImpGraph)
        case CfgFields    => Some(Page.CfgFields)
        case CfgIssues    => Some(Page.CfgIssues)
        case CfgReqTypes  => Some(Page.CfgReqTypes)
        case CfgTags      => Some(Page.CfgTags)
      } {
        case Page.ReqTable     => Some(ReqTable)
        case Page.ReqDetail(_) => Some(ReqDetail)
        case Page.ImpGraph     => Some(ImpGraph)
        case Page.CfgFields    => Some(CfgFields)
        case Page.CfgIssues    => Some(CfgIssues)
        case Page.CfgReqTypes  => Some(CfgReqTypes)
        case Page.CfgTags      => Some(CfgTags)
        case Page.Index        => None
      }
  }

  sealed abstract class Category(final val title     : String,
                                 final val icon      : Icon,
                                 final val cardColour: Colour,
                                 final val iconColour: Colour,
                                 final val items     : NonEmptyVector[Item])
  object Category {
    import Item._

    case object Content extends Category(
      "Content", Icon.FileTextOutline, Colour.Blue, Colour.Default,
      NonEmptyVector(ReqTable, ReqDetail, ImpGraph))

    case object Configuration extends Category(
      "Configuration", Icon.Setting, Colour.Yellow, Colour.Grey,
      NonEmptyVector(CfgFields, CfgIssues, CfgReqTypes, CfgTags))

    implicit def univEq: UnivEq[Category] = UnivEq.derive

    //val All = UtilMacros.adtValuesManual[Category](
    val All = NonEmptyVector[Category](
      Content, Configuration)
  }

  def dropdownItems(active: Option[Item], rc: RouterCtl): Dropdown.Items =
    Category.All.iterator.flatMap(c =>
      Iterator.single(Dropdown.Item.Header(c.title)) ++
      c.items.iterator.flatMap(i =>
        if (active.exists(_ ==* i))
          Dropdown.Item.Div(i.title, Dropdown.ItemState.Active) :: Nil
        else Item.ToPage.getOption(i) match {
          case Some(p) => Dropdown.Item.Link(rc.link(p)(i.title)) :: Nil
          case None    => Nil
        }
      )
    ).toList

  /*
      %h3.ui.dividing.header
        %i.icon.file.text.outline
        .content Content

      .ui.cards.three

        .ui.card.blue{onclick: "location.href='project-reqtable.haml.html'", style: "cursor:pointer"}
          .content.pic
            %i.icon.cubes
            -# %i.icon.table
          .content
            .header Req Table
            .description View and edit all reqs.

        .ui.card.blue
          .content.pic.blurring.omg-dimmable
            .ui.inverted.dimmer
              .content
                .center
                  .ui.search
                    .ui.icon.input
                      %input.prompt{type: "text", size: "18"}/
                      %i.icon.search
            %i.icon.cube
            -# %i.icon.file.text.outline
          .content
            .header Req Lookup
            .description View and edit a single req.

        .ui.card.blue
          .content.pic
            %i.icon.share.alternate
          .content
            .header Implication Graph
            .description ???

      %h3.ui.dividing.header
        %i.icon.setting
        -# %i.icon.ellipsis.vertical
        .content Configuration

      .ui.cards.three

        .ui.card.yellow
          .content.pic
            %i.icon.list.layout.grey
          .content
            .header Fields
            .description ???

        .ui.card.yellow
          .content.pic
            %i.icon.warning.sign.grey
          .content
            .header Issues
            .description ???

        .ui.card.yellow
          .content.pic
            %i.icon.inbox.grey
          .content
            .header Req Types
            .description ???

        .ui.card.yellow
          .content.pic
            %i.icon.tags.grey
          .content
            .header Tags
            .description ???

  :javascript
    $('.omg-dimmable').dimmer({on: 'hover'});
   */

  val Component = ReactComponentB[RouterCtl]("ProjectHome")
    .render_P { ctl =>
      import Page._
      <.ul(
        Vector(ReqTable, ImpGraph, CfgFields, CfgIssues, CfgReqTypes, CfgTags).map(p =>
          <.li(ctl.link(p)(p.toString))))
    }
    .build
}

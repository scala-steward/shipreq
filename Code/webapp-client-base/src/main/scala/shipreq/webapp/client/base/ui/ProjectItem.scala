package shipreq.webapp.client.base.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.URLs
import shipreq.webapp.base.UiText.EnglishStringExt
import shipreq.webapp.base.data.ProjectCatalogue
import shipreq.webapp.client.base.jsfacade.MomentJs
import shipreq.webapp.client.base.ui.semantic.{Icon, Size, Statistic, StatisticGroup}
import BaseStyles.{projectItems => *}

/** Project name and summary.
  *
  * +-----------------------------------------------+
  * | Pacman                              2090  503 |
  * | Updated 30 years ago.            CHANGES REQS |
  * +-----------------------------------------------+
  */
object ProjectItem {

  type Props = ProjectCatalogue.Item

  val statGroupStyle = StatisticGroup.Style(Size.Tiny)

  def stat(i: Icon, n: Int, s: String) =
    Statistic.simple(TagMod(i.tag, " ", n), s.pluralise(n))

  def render(p: Props): ReactElement = {

    val header =
      <.h1(*.itemHeader,
        <.a(^.href := URLs.PageProject(p.id), p.name))

    val meta =
      <.div(*.itemMeta,
        "Updated ",
        TimeAgo.Component(MomentJs fromInstant p.lastUpdatedOrCreatedAt),
        ".")

    val stats =
      StatisticGroup.Props(
        statGroupStyle,
        stat(Icon.Write, p.eventCount, "change") ::
        stat(Icon.Cubes, p.reqCount, "req") :: Nil
      ).render

    <.div(*.item,
      <.div(*.itemLeft, header, meta),
      <.div(stats))
  }

  val Component = FunctionalComponent(render)
}

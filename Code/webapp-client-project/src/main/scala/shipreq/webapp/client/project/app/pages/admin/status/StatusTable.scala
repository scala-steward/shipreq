package shipreq.webapp.client.project.app.pages.admin.status

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import shipreq.webapp.base.data.Rolodex
import shipreq.webapp.base.ui.semantic.{Icon, Table}
import shipreq.webapp.member.jsfacade.MomentJs
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event.VerifiedEvent
import shipreq.webapp.member.project.util.DataReusability._
import shipreq.webapp.member.ui.TimeAgo

object StatusTable {

  final case class Props(project: Project,
                         rolodex: Rolodex,
                         meta   : ProjectMetaData) {
    @inline def render: VdomElement = Component(this)
  }

  object Props {
    implicit val reusability: Reusability[Props] =
      Reusability.derive
  }

  private def render(p: Props) = {

    def row(header: VdomNode, body: VdomNode) =
      <.tr(
        <.td(header),
        <.td(body))

    def event(e: VerifiedEvent) =
      <.div(
        TimeAgo.Component(MomentJs.fromInstant(e.createdAt)),
        " by ",
        p.rolodex.need(e.author).with_@)

    var reqs: VdomNode =
      p.meta.reqsLive

    if (p.meta.reqsDead > 0)
      reqs = <.div(
        reqs,
        " ( +",
        p.meta.reqsDead,
        Icon.TrashOutline.tag,
        "= ",
        p.meta.reqsTotal,
        " )",
      )

    Table.definition(
      <.tbody(
        row("Created", event(p.project.history.events.head)),
        row("Last updated", event(p.project.history.events.last)),
        row("Changes", p.meta.eventsPostInit),
        row("Reqs", reqs),
      ),
    )
  }

  val Component = ScalaComponent.builder[Props]
    .render_P(render)
    .configure(Reusability.shouldComponentUpdate)
    .build
}

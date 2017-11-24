package shipreq.webapp.client.project.feature.deletion

import japgolly.microlibs.nonempty._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalacss.ScalaCssReact._
import shipreq.base.util._
import shipreq.webapp.base.UiText
import shipreq.webapp.base.data._
import shipreq.webapp.base.protocol.UpdateContentCmd.RestoreContent
import shipreq.webapp.base.text.{PlainText, TextSearch}
import shipreq.webapp.base.ui.semantic.{Button, Colour, Icon, Table}
import shipreq.webapp.client.project.app.Style.{restorationForm => *}
import shipreq.webapp.client.project.app.TestMarker
import shipreq.webapp.client.project.feature.Selection
import shipreq.webapp.client.project.widgets.{ProjectWidgets, Widgets}

object RestorationForm {
  import DeletionRestorationLogic.{Data, ReqRow}

  final case class Props(data   : Data,
                         widgets: ProjectWidgets.NoCtx,
                         perform: RestoreContent => Callback,
                         cancel : Callback) {
    def render: VdomElement = Component(this)
  }

  type State = Selection[ReqId]

  def stateInit(p: Props): State =
    Selection(p.data.initialReqs)

  final class Backend($: BackendScope[Props, State]) {
    private val setReqSel = Reusable.fn.state($).set

    private def renderReqTable(p: Props, selectedReqs: State): VdomElement = {
      val project        = p.data.project
      val customReqTypes = project.config.reqTypes
      val selection      = selectedReqs.updateBy(setReqSel).legal(p.data.optionalReqIds)

      val header: VdomTag =
        <.thead(
          <.tr(
            <.th(^.rowSpan := 2, *.reqTableSelCol, selection.total.checkboxAndOnClick),
            <.th(^.rowSpan := 2, UiText.ColumnNames.pubid),
            <.th(^.rowSpan := 2, UiText.ColumnNames.title),
            <.th(^.colSpan := 2, *.reqTableHeaderImpsTop, UiText.ColumnNames.implications(Backwards))),
          <.tr(
            <.th(
              *.reqTableHeaderImpsBottomLeft,
              Icon.TrashOutline.withColour(Colour.Red).tag(*.reqTableHeaderImpsIcon)),
            <.th(
              *.reqTableHeaderImpsBottomRight,
              Icon.Unhide.tag(*.reqTableHeaderImpsIcon))))

      val liveGivenState: Req => Live =
        r => Live when selectedReqs.selected.contains(r.id)

      val renderImpliedByItem =
        p.widgets.PubidFormat(Plain, *.reqTableImps(_), liveFn = liveGivenState)

      def reqRow(rr: ReqRow): VdomTag = {
        val req: Req = rr.req
        val id: ReqId = rr.req.id
        val live: Live = liveGivenState(req)

        val sel: TagMod =
          if (selection.legal contains id)
            selection(rr.req.id).checkboxAndOnClick
          else
            Widgets.checkboxReadOnly(On)

        val pubidStr: String =
          PlainText.pubid(req.pubid, project)

        val indentedPubid: TagMod =
          if (rr.indent ==* 0)
            <.div(*.pubid(live), pubidStr)
          else
            TagMod(
              <.div(^.width := *.indentWidth(rr.indent)),
              <.div(*.reqTableTreeIndicator, "↳"),
              <.div(*.pubid(live), pubidStr))

        val imps: Live.Values[VdomTag] =
          Live.Values
            .partition[Vector, Req](rr.impliedBy)(liveGivenState)
            .map(renderImpliedByItem.reqs)

        <.tr(
          *.reqTableRow(live),
          ^.key := id.value,
          <.td(*.reqTableSelCol, sel),
          <.td(*.reqTablePubidCell, indentedPubid),
          <.td(*.reqTableTitle(live), p.widgets reqTitle rr.req),
          <.td(*.reqTableImpsCell, imps(Dead)),
          <.td(*.reqTableImpsCell, imps(Live)))
      }

      Table.celledCompactUnstackable(
        *.reqTable,
        header,
        <.tbody(p.data.actionableReqs.toVdomArray(reqRow)))
    }

    private val cancelButton: VdomTag =
      Button(
        tipe = Button.Type.BasicIconAndText(Icon.Remove, UiText.buttonAbortChange),
        colour = Colour.Black
      ).tag(^.onClick --> $.props.flatMap(_.cancel))

    def render(p: Props, selectedReqs: State): VdomElement = {
      assert(p.data.actionableGroups.isEmpty,
        "Since proper UI/UX implementation, Restoration no longer accepts deletable code-groups")

      val commit: Option[Callback] =
        for {
          reqs ← NonEmptySet.option(selectedReqs.selected)
        } yield p.perform(RestoreContent(reqs.whole, Set.empty))

      val restoreButton: VdomTag =
        Button(
          tipe = Button.Type.BasicIconAndText(Icon.Undo, UiText.Life.restore),
          state = Button.State.enabledWhen(commit.isDefined),
          colour = Colour.Green
        ).tag(^.onClick -->? commit)

      <.main(
        *.main,
        TestMarker.restorationForm.tagMod,
        <.h2("You are about to restore the following requirements:"),
        <.section(
          <.div(*.reqHelp, "In addition to those you selected, implied requirements are also presented with exclusively-implied requirements auto-selected for restoration."),
          renderReqTable(p, selectedReqs)),
        <.div(*.bottomSection,
          cancelButton,
          <.div(*.buttonGap), // curse Semantic UI!
          restoreButton))
    }
  }

  val Component = ScalaComponent.builder[Props]("Restoration")
    .initialStateFromProps(stateInit)
    .renderBackend[Backend]
    .build

}

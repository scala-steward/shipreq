package shipreq.webapp.client.project.app.reqtable2

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.scalajs.react._
import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.univeq._
import monocle.Lens
import monocle.macros.Lenses
import org.scalajs.dom.document
import scalacss.ScalaCssReact._
import shipreq.base.util.Allow
import shipreq.webapp.base.data._
import shipreq.webapp.base.filter.ValidFilter
import shipreq.webapp.base.protocol.UpdateContentCmd
import shipreq.webapp.base.text.{PlainText, TextSearch}
import shipreq.webapp.client.base.feature.AsyncFeature
import shipreq.webapp.client.base.lib.DataReusability._
import shipreq.webapp.client.base.ui.BaseStyles
import shipreq.webapp.client.project.app.state.ClientData
import shipreq.webapp.client.project.app.Style.reqtable2.{page => *}
import shipreq.webapp.client.project.feature._
import shipreq.webapp.client.project.protocol.ServerCall
import shipreq.webapp.client.project.widgets.ProjectWidgets

object ReqTablePage {

  def apply(staticProps: StaticProps) =
    ScalaComponent.builder[Props]("ReqTablePage")
      .backend(new Backend(staticProps, _))
      .renderBackend
      .componentWillMount(_.backend.syncState)
      .componentDidMount(_.backend.unfocus)
      .componentWillReceiveProps($ => $.backend.onPropsChange($.currentProps, $.nextProps))
      .build

  final case class StaticProps(stateAccess     : StateAccessPure[State],
                               cd              : ClientData,
                               pxPlainText     : Px[PlainText.ForProject],
                               pxTextSearch    : Px[TextSearch],
                               pxProjectWidgets: Px[ProjectWidgets],
                               reqDetailRC     : RouterCtl[ExternalPubid],
                               updateIO        : ServerCall[UpdateContentCmd],
                               rowAsyncW       : AsyncFeature.Write.D1[Row.SourceId, String])

  final case class Props(create    : CreateFeature.ReadWrite.ForProject,
                         editor    : EditorFeature.ReadWrite.ForProject,
                         rowAsync  : AsyncFeature.Read.D1[Row.SourceId, String],
                         filterDead: StateSnapshot[FilterDead],
                         state     : State)

  @Lenses
  final case class State(tableSettings: TableSettings,
                         filter       : FilterEditor.State,
                         selection    : RowSelection,
                         newStuff     : NewStuff.State,
                         modal        : Modal.State)

  object State {
    def init: State =
      State(
        TableSettings.default,
        FilterEditor.State.init,
        Selection.empty,
        NewStuff.State.init,
        Modal.none)

    val validFilter: Lens[State, Option[ValidFilter]] =
      tableSettings ^|-> TableSettings.filter

    val sortCriteria: Lens[State, SortCriteria] =
      tableSettings ^|-> TableSettings.order
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  final class Backend(sp: StaticProps, $: BackendScope[Props, Unit]) {
    import sp._
    import cd.pxProject

    val modSettings    : ModFn[TableSettings ] = Reusable.fn.state(stateAccess zoomStateL State.tableSettings).mod
    val setNewStuff    : SetFn[NewStuff.State] = Reusable.fn.state(stateAccess zoomStateL State.newStuff).set
    val setSelection   : SetFn[RowSelection  ] = Reusable.fn.state(stateAccess zoomStateL State.selection).set
    val setSortCriteria: SetFn[SortCriteria  ] = Reusable.fn.state(stateAccess zoomStateL State.sortCriteria).set
    val setModal       : SetFn[Modal.State   ] = Reusable.fn.state(stateAccess zoomStateL State.modal).set

    private var manualRefresh = List.empty[Px.ThunkM[_]]
    private def pxProps[A: Reusability](f: Props => A): Px.ThunkM[A] = {
      val px = Px.props($).map(f).withReuse.manualRefresh
      manualRefresh ::= px
      px
    }

    val pxFilterDead                                = pxProps(_.filterDead.value)
    val pxTableSettings: Px[TableSettings         ] = pxProps(_.state.tableSettings)
    val pxSelection    : Px[RowSelection          ] = pxProps(_.state.selection)
    val pxActiveColumns: Px[NonEmptyVector[Column]] = pxProps(_.state.tableSettings.columns)

    val pxRows: Px[Vector[Row]] =
      for {
        p  <- pxProject
        s  <- pxTableSettings
        fd <- pxFilterDead
        pt <- pxPlainText
        ts <- pxTextSearch
      } yield Logic.rowsForTable(p, s, fd, pt, ts)

    val pxRowIdsWithWholeRowAsync: Px[Set[Row.SourceId]] =
      pxProps(_.rowAsync.keySet)

    /** Rows which the user has selected that:
      * - are currently visible (i.e. ignoring filtered out)
      * - aren't currently busy with some async action (in which case the selection checkbox is replaced with a spinner)
      */
    val pxRowSelectionVisible: Px[RowSelectionVisible] =
      for {
        rs <- pxRows
        wr <- pxRowIdsWithWholeRowAsync
        s  <- pxSelection
      } yield
        s.updateBy(setSelection).legal(rs.iterator.map(_.sourceId).toSet &~ wr)

    val pxActiveColumnsPlus: Px[NonEmptyVector[ColumnPlus]] =
      (for {
        p  <- pxProject
        cs <- pxActiveColumns
      } yield ColumnPlus.forceNEV(ColumnPlus.byProject(p))(cs))
        .withReuse

    val pxColumnPlusAll: Px[ColumnPlus.All] =
      (for {
        p  <- pxProject
        fd <- pxFilterDead
      } yield ColumnPlus.All(p, fd))
        .withReuse

    val pxColumnSelector: Px[VdomElement] =
      for {
        sel <- pxActiveColumns
        all <- pxColumnPlusAll
      } yield
        ColumnSelector.Props(sel, all, modSettings.map(m => u => m(_.setColumns(u)))).render

    val onFilterChange: FilterEditor.UpdateFn =
      (newState, newFilter) => stateAccess.modState { oldState =>
        var f = State.filter.set(newState)
        if (newFilter !=* oldState.tableSettings.filter)
          f = f compose State.validFilter.set(newFilter)
        f(oldState)
      }

    val pxTableContentStats: Px[TableContentStats] =
      for {
        p    <- pxProject
        rows <- pxRows
      } yield Logic.stats(p, rows)

    val pxPageSummary: Px[VdomElement] =
      for {
        fd    <- pxFilterDead
        stats <- pxTableContentStats
        sel   <- pxRowSelectionVisible
      } yield {
        // `legalSelection` because the same sourceId can appear more than once
        val totalSelected = sel.legalSelectionSize
        PageSummary.Props(stats, totalSelected, fd).render
      }

    val pxSortCriteriaEditor: Px[VdomElement] =
      for {
        s <- pxTableSettings
        c <- pxColumnPlusAll
      } yield SortCriteriaEditor.Props(s.order, setSortCriteria, c).render

    val pxSelectionCtrls: Px[SelectionCtrls.Props] =
      for {
        project        <- pxProject
        projectWidgets <- pxProjectWidgets
        projectText    <- pxPlainText
        textSearch     <- pxTextSearch
        rows           <- pxRows
        sel            <- pxRowSelectionVisible
      } yield SelectionCtrls.Props(
        sel, rows, setModal, project, projectWidgets, projectText, textSearch, updateIO, rowAsyncW)

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    def render(p: Props): VdomElement = {
      Px.refresh(manualRefresh: _*)
      p.state.modal renderOrElse renderMain(p)
    }

    def renderMain(p: Props): VdomElement = {
      val activeColumnsPlus = pxActiveColumnsPlus.value()

      val newStuff = new NewStuff(
        p.state.newStuff,
        setNewStuff,
        pxProject.value().config.reqTypes,
        Allow when p.state.tableSettings.viewCodeGroups,
        p.create,
        activeColumnsPlus)

      val filterEditor = FilterEditor.Props(
        p.state.filter,
        pxProject.value(),
        onFilterChange,
      ).render

      val table = Table.Whole.Props(
        pxRows.value(),
        activeColumnsPlus,
        pxRowSelectionVisible.value(),
        p.editor,
        p.rowAsync,
        pxProject.value().config,
        pxProjectWidgets.value(),
        modSettings,
      ).render

      <.main(BaseStyles.containerFull,

        ViewsMenu.Component(p.filterDead),

        <.div(*.actionCtrls,
          newStuff.buttonProps.render,
          pxSelectionCtrls.value().render,
          <.div(*.summary, pxPageSummary.value())),

        <.div(*.viewCtrls,
          pxSortCriteriaEditor.value(),
          <.div(*.flexGap),
          filterEditor,
          pxColumnSelector.value()),

        newStuff.form.whenDefined,

        table)
    }

    def onPropsChange(prev: Props, next: Props): Callback =
      if (prev.filterDead.value ==* next.filterDead.value)
        Callback.empty
      else
        syncState

    /** Synchronises the State of this page with external Props that affect it. */
    val syncState: Callback =
      stateAccess.modState { s =>
        pxFilterDead.refresh()
        State.tableSettings.modify(_.filterColumns(pxColumnPlusAll.value().containsColumn))(s)
      }

    // Prevent browser auto-focusing the first <input> it sees on page load
    def unfocus = Callback {
      document.activeElement.domToHtml.foreach(_.blur())
    }
  }

}

package shipreq.webapp.client.project.feature.savedview

import japgolly.scalajs.react.Reusability
import monocle.Optional
import monocle.macros.Lenses
import monocle.std.option.pSome
import shipreq.base.util.Valid
import shipreq.webapp.base.data.{FilterDead, Project, ShowDead}
import shipreq.webapp.base.data.savedview.View
import shipreq.webapp.base.feature.AsyncFeature
import shipreq.webapp.base.filter.Filter
import shipreq.webapp.base.lib.DataReusability._
import shipreq.webapp.client.project.feature.EditorFeature
import shipreq.webapp.client.project.widgets.FilterEditor

@Lenses
final case class State(view  : ViewLogic.State,
                       filter: FilterEditor.State,
                       async : AsyncFeature.State.D0[EditorFeature.AsyncError]) {

  def setFilterDead(fd: FilterDead, p: Project): State = {
    val showingBuiltInDefault = p.savedViews.isEmpty && view.manualView.isEmpty
    if (showingBuiltInDefault)
      this
    else {
      val v1 = view.activeView(p.savedViews, fd)
      val v2 = v1.copy(filterDead = fd)
      State.setModifiedView(p, updateFilterText = false)(v2)(this)
    }
  }

}

object State {
  def init: State =
    apply(
      view   = ViewLogic.State.init,
      filter = FilterEditor.State.init,
      async  = AsyncFeature.State.initD0,
    )

  def init(p: Project): State =
    updateFilterText(p)(init)

  implicit def reusability: Reusability[State] =
    Reusability.byRef || Reusability.derive

  val manualView: Optional[State, View] =
    view ^|-> ViewLogic.State.manualView ^<-? pSome


  def modifyView(project           : Project,
                 filterDeadFallback: FilterDead,
                 updateFilterText  : Boolean)
                (modify            : View => View): State => State =
    s => {
      val newView = modify(s.view.activeView(project.savedViews, filterDeadFallback))
      setModifiedView(project, updateFilterText)(newView)(s)
    }

  def setModifiedView(project         : Project,
                      updateFilterText: Boolean)
                     (newView         : View): State => State =
    runSavedViewAction(project, updateFilterText)(ViewLogic.Action.Modify(newView))

  def runSavedViewAction(project         : Project,
                         updateFilterText: Boolean)
                        (action          : ViewLogic.Action): State => State = {
    val modSVS   = ViewLogic.Action.interpret(project.savedViews)(action)
    val modState = view.modify(modSVS)
    if (updateFilterText)
      modState andThen this.updateFilterText(project)
    else
      modState
  }

  def updateFilterText(project: Project): State => State = s => {
    val filterDeadFallback = ShowDead // This doesn't impact filter text
    val v = s.view.activeView(project.savedViews, filterDeadFallback)
    val txt = v.filter.fold("")(Filter.Valid.toText(project.config, _))
    filter.set(FilterEditor.State(txt, Valid))(s)
  }
}
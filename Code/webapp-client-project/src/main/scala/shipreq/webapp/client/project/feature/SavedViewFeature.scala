package shipreq.webapp.client.project.feature

import japgolly.scalajs.react.Reusability
import japgolly.scalajs.react.vdom.VdomElement

/** Usage
  * =====
  *
  * Super easy.
  *
  * 1. (Optional) Request an instance of [[SavedViewFeature.Static]] in your static props if you want to chain a bunch
  *    of `Px` instances from the saved view `Px`s.
  *
  * 2. Request an instance of [[SavedViewFeature]] in your props.
  *
  * 3. Call [[SavedViewFeature.renderSavedViewManager()]] and/or [[SavedViewFeature.renderFilterEditor()]] for managed
  * component instances.
  */
object SavedViewFeature {

  val ColumnLogic = savedview.ColumnLogic

  type ColumnPlus = shipreq.webapp.client.project.feature.savedview.ColumnPlus
  val  ColumnPlus = shipreq.webapp.client.project.feature.savedview.ColumnPlus

  type State = savedview.State
  val  State = savedview.State

  type Static = savedview.Static
  val  Static = savedview.Static

  implicit val reusability: Reusability[SavedViewFeature] =
    Reusability.byRef || Reusability.derive
}

final case class SavedViewFeature(static: SavedViewFeature.Static,
                                  state : SavedViewFeature.State) {

  def renderSavedViewManager(): VdomElement =
    static.renderSavedViewManager(state.async)

  def renderFilterEditor(): VdomElement =
    static.renderFilterEditor(state)
}
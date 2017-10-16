package shipreq.webapp.client.project.app

import japgolly.scalajs.react._
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.extra._
import shipreq.base.util.univeq._
import shipreq.webapp.client.project.feature.Selection

package object reqtable {

  type Column = shipreq.webapp.base.data.reqtable.Column
  val  Column = shipreq.webapp.base.data.reqtable.Column

  type SortMethod = shipreq.webapp.base.data.reqtable.SortMethod
  val  SortMethod = shipreq.webapp.base.data.reqtable.SortMethod

  type SortCriterion = shipreq.webapp.base.data.reqtable.SortCriterion
  val  SortCriterion = shipreq.webapp.base.data.reqtable.SortCriterion

  type SortCriteria = shipreq.webapp.base.data.reqtable.SortCriteria
  val  SortCriteria = shipreq.webapp.base.data.reqtable.SortCriteria

  type SetFn[A] = A ~=> Callback
  type ModFn[A] = (A => A) ~=> Callback

  type RowSelection        = Selection[Row.SourceId]
  type RowSelectionVisible = Selection.LegalWithUpdateFn[Row.SourceId]

  @inline implicit def ColumnImplicitExt(o: Column.type) = ColumnExt

  implicit val reusabilityColumn       : Reusability[Column       ] = Reusability.byEqual
  implicit val reusabilitySortCriterion: Reusability[SortCriterion] = Reusability.byRefOrEqual
  implicit val reusabilitySortCriteria : Reusability[SortCriteria ] = Reusability.byRefOrEqual

  @inline def shouldComponentUpdate[P: Reusability, C <: Children, S: Reusability, B]: ScalaComponent.Config[P, C, S, B] =
    shipreq.webapp.client.project.app.shouldComponentUpdate[P, C, S, B]
//   Reusability.shouldComponentUpdateWithOverlay[P, C, S, B]

}

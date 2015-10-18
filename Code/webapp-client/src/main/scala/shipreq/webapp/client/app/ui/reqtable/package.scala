package shipreq.webapp.client.app.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import shipreq.webapp.client.lib.TCB

/**
 * Requirements Table.
 * "Common Req View & Editor" in the prototype.
 *
 * An Excel-like table for reading and editing requirements.
 */
package object reqtable {

  type Rows = Vector[Row]
  implicit val reusabilityRows: Reusability[Rows] = Reusability.byRef // Each row will be checked anyway

  type RowSelection        = Selection[Row.SourceId]
  type RowSelectionVisible = Selection.VisibleWithUpdateFn[Row.SourceId]

  type CallServer[-I] = (I, TCB.Success, String => TCB.Failure) => Callback
  implicit def callServerReusability[I] = Reusability.byRef[CallServer[I]] // All are vals in ReqTable

  @inline def shouldComponentUpdate[P: Reusability, S: Reusability, B, N <: TopNode] =
    shipreq.webapp.client.app.ui.shouldComponentUpdate[P, S, B, N]
    // Reusability.shouldComponentUpdateWithOverlay[P, S, B, N]
}

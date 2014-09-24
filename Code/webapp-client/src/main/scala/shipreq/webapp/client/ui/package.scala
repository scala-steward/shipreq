package shipreq.webapp.client

import japgolly.scalajs.react._
import org.scalajs.dom.HTMLInputElement
import shipreq.webapp.shared.validation.VFailure

package object ui {

  // TODO delete
  type ErrorMsg = String

  type InputEvent = SyntheticEvent[HTMLInputElement]

  type ValidateR[S, R, O] = R => ValidateS[S, O]
  type ValidateS[S, O] = (S, O) => Option[VFailure]
}
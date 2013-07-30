package com.beardedlogic.usecase
package snippet.uce

import scala.xml.{Text, NodeSeq}
import scalaz.{Memo, NonEmptyList}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.jquery.JqJE
import net.liftweb.http.js.jquery.JqJsCmds.jsExpToJsCmd
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import JsCmds.Noop

import lib.Types._
import lib.change._
import lib.field._
import lib.{UcChangeSource, UseCase}
import model._
import util.JsExt._
import Changes._

trait RendererHelper {
  val uce: UseCaseEditor

  @inline final def textFieldIds = uce.textFieldIds
  @inline final def state = uce.state
  @inline final def uch = state.uc.header
  @inline final def fields = state.uc.fields
  @inline final implicit def fieldValues = state.uc.fieldValues

  // % as in "mod(ify)"
  @inline final def %(f: UseCase => UcUpdateResult): JsCmd = uce.update(f)
  @inline final def =>%(f: UseCase => UcUpdateResult) = () => uce.update(f)

  @inline final def withDao[R](f: DAO => R): R = uce.daoProvider.withTransaction(f)
  @inline final def daoCallback[R](f: DAO => R) = () => withDao(f)
}

object Renderer {
  def TitleId = "uc-title"
}

case class Renderer(uce: UseCaseEditor) extends RendererHelper {

  import Renderer._

  // *************************************
  // *             Rendering             *
  // *************************************


  def render = (
    ".ucdata *" #> renderFields andThen
      ".title .ucid *" #> uch.number.toString
        & ".rev *" #> state.currentRevision
        & ".title @title" #> SHtml.ajaxText(uch.title, i => %(_.updateTitle(i)), "id" -> TitleId)
        & ".saveUseCase" #> SHtml.ajaxButton("Save", daoCallback(uce.onSave))
    )

  def renderFields: NodeSeq =
    (NodeSeq.Empty /: fields.map(renderField))(_ ++: _)

  def renderField(f: Field): NodeSeq = f match {
    case tf: TextField => renderTextField(tf)(Templates.TextField)
    case sf: StepField => stepRenderers(sf).render
    case _ => NodeSeq.Empty
  }

  def renderTextField(f: TextField) = (
    "th *" #> f.defn.title
      & "textarea" #> SHtml.ajaxTextarea(f.value.text, i => %(f.updateText(i)), "id" -> textFieldIds(f))
    )

  val stepRenderers = Memo.immutableListMapMemo[StepField, StepFieldRenderer] {
    case f: NormalCourseField => StepFieldRenderer(uce, f, NormalCourseFieldConfig)
    case f: ExceptionCourseField => StepFieldRenderer(uce, f, ExceptionCourseFieldConfig)
  }


  // **************************************
  // *             Javascript             *
  // **************************************

  def jsRespondChangeFailure(errorMessage: String): JsCmd =
    JsCmds.Alert(errorMessage)

  def jsRespondToChanges(changes: NonEmptyList[(UcChangeSource, Change)]): JsCmd = {
    var js = Noop
    for (c <- changes.list) {
      c match {
        case (_,            TitleChanged(_, _))                 => js &= jsUpdateTitle
        case (f: TextField, TextChanged)                        => js &= jsUpdateTextField(f)
        case (f: StepField, StepTextChanged(id))                => js &= stepRenderers(f).jsUpdateStepFieldText(id)
        case (f: StepField, TailStepAdded(node))                => js &= stepRenderers(f).jsAddTailStep(node)
        case (f: StepField, StepAdded(precedingId, node))       => js &= stepRenderers(f).jsAddStep(precedingId, node)
        case (f: StepField, StepRemoved(node))                  => js &= stepRenderers(f).jsRemoveStep(node)
        case (f: StepField, StepIndentIncreased(node, oldTree)) => js &= stepRenderers(f).jsIncIndent(node, oldTree)
        case (f: StepField, StepIndentDecreased(node, _))       => js &= stepRenderers(f).jsDecIndent(node)
        case _ =>
      }
    }
    js
  }

  def jsUpdateRevision: JsCmd =
    JqExpr(".rev") ~> JqJE.JqHtml(Text(state.currentRevision))

  def jsUpdateTitle: JsCmd =
    JqId(TitleId) ~> JqSetValue(uch.title, false)

  def jsUpdateTextField(f: TextField): JsCmd =
    JqId(textFieldIds(f)) ~> JqSetValue(f.value.text, false)
}
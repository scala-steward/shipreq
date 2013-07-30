package com.beardedlogic.usecase
package snippet.uce

import scala.xml.{Text, NodeSeq}
import net.liftweb.common._
import net.liftweb.http.js.JsExp.strToJsExp
import net.liftweb.http.js.{JE, JsCmd, JsCmds}
import net.liftweb.http.js.jquery.JqJE
import net.liftweb.http.js.jquery.JqJsCmds.jsExpToJsCmd
import net.liftweb.http._
import net.liftweb.util.Helpers._
import JsCmds.Noop

import com.beardedlogic.usecase.lib._
import lib.field._
import model._
import lib.Types._
import com.beardedlogic.usecase.util.JsExt.JqExpr
import com.beardedlogic.usecase.lib.text.FreeText
import net.liftweb.util.CssSel
import com.beardedlogic.usecase.lib.change._

case class State(uc: UseCase, prevSave: Option[UseCaseSaveCheckpoint]) {
  def currentRevision = prevSave.map(_.rec.rev.toString).getOrElse("0")
}

object UseCaseEditor {
  // TODO Delete UCE . initial state
  val InitialState: State = {
    val h = UseCaseHeader(Defaults.Title, 1)
    val fl = Defaults.FieldList.get.fields
    val ncf = UseCaseFns.filter[NormalCourseField](fl).head
    val nct = StepTree(StepNodeBuilder(0, 0, List(StepNodeBuilder(1, 1))) :: Nil)
    val fv = fl.map(f => (f ~> f.empty)).toMap + (ncf ~> StepFieldValue.forTree(ncf, nct))
    val sl = UseCaseFns.generateStepAndLabelBiMap(fv, h)
    val uc = UseCase(h, fl, fv, sl)
    State(uc, None)
  }
}

class UseCaseEditor extends StatefulSnippet with SnippetHelpers {

  import UseCaseEditor._

  private var state__ = InitialState
  private var renderer__ = newRenderer

  @inline final def state = state__
  @inline final def renderer = renderer__
  @inline final def uc = state.uc
  @inline final def uch = uc.header
  @inline final def fields = uc.fields
  @inline final def fieldValues = uc.fieldValues

  protected def setState(newState: State): Unit = {
    state__ = newState
    renderer__ = newRenderer
  }

  val textFieldIds: Map[Field, LocalIdStr] =
    UseCaseFns.filter[TextField](fields)
    .map(f => (f -> nextFuncName.asLocalId))
    .toMap

  def newRenderer = Renderer(this)

  override def dispatch = { case _ => renderer.render }

  def update(f: UseCase => UcUpdateResult): JsCmd =
    f(uc) match {
      case Changed(newUc, changes) =>
        setState(State(newUc, state.prevSave))
        renderer.jsRespondToChanges(changes)

      case NoChange => Noop

      case ChangeFailure(err) => renderer.jsRespondChangeFailure(err)
    }

  def onSave(dao: DAO): JsCmd = {
    UseCaseFns.save(uc, state.prevSave, dao) match {
      case thisSave@Some(cp) =>
        setState(State(cp.uc, thisSave))
        renderer.jsUpdateRevision
      case None => Noop
    }
  }
}

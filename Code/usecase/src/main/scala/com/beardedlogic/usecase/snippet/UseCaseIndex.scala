package com.beardedlogic.usecase
package snippet

import net.liftweb.util.Helpers._
import net.liftweb.util.ClearClearable
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.SHtml
import com.beardedlogic.usecase.lib.msg.{JavaScript, Reactor, JavaScriptReaction}
import com.beardedlogic.usecase.lib.db.DB
import com.beardedlogic.usecase.model.{PlainValue, UseCaseSummary, UseCaseWithValue, DAO}
import com.beardedlogic.usecase.lib.{KnockoutJs, TemplateCache, SnippetHelpers, Defaults}
import TemplateCache._
import com.beardedlogic.usecase.lib.JsExt._
import net.liftweb.json.Serialization.{write => jsonWrite}
import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import scala.xml.Node

object UseCaseIndex extends SnippetHelpers {

  //  val EditTemplate = UseCaseIndexTemplate.extract("template-edit")

  private implicit val jsonFormats = Serialization.formats(NoTypeHints)
  def modelInit(knockoutModelName: String, model: AnyRef): Node = {
    val json = jsonWrite(model)
    val js = KnockoutJs.ApplyBindings(knockoutModelName, json)
    JsCmds.Script(js)
  }

  def render = DAO.withSession(dao =>
    ClearClearable
      & "#modelInit" #> modelInit("UseCaseIndexModel", dao.findAllUseCaseSummaries)
      & ".new_uc button" #> SHtml.ajaxButton("+ New UC", jsCallbackWithDao(createNewUseCase))
  )

  def createNewUseCase(reactor: Reactor, dao: DAO): UseCaseWithValue = {
    val uc = dao.createInitialUseCase(Defaults.Title, Defaults.FieldList.get)
    //    val uc = UseCaseWithValue(PlainValue(1, 2, 3), "Ahh", 4, 1000)
    reactor(JavaScript)(
      //      JqExpr(".uc_list") ~> JqAppend(renderUseCaseEditor(uc)(EditTemplate))
      //        & JqId(editorId(uc)) ~> JqHide ~> JqFadeIn()
      // System appends new UC to UC list.
      // Title is a text input box, and focused.
    )
    uc
  }

  //  def renderUseCaseEditor(uc: UseCaseWithValue) = {
  //    var title = uc.title
  //    (".UC-num" #> uc.number.toString
  //      & ".title textarea" #> SHtml.textarea(title, title = _)
  //      & ".save" #> SHtml.ajaxButton("Save", jsCallbackWithDao(updateUseCase(uc, title)))
  //      & ".edit [id]" #> editorId(uc)
  //      )
  //  }
  //
  //  def editorId(uc: UseCaseWithValue) = "u" + uc.valueId
  //
  //  def updateUseCase(uc: UseCaseWithValue, newTitle: String)(reactor: Reactor, dao: DAO): Unit = {
  //    reactor(JavaScript)(JsCmds.Alert(s"From '${uc.title}' to '$newTitle}'"))
  //  }
}

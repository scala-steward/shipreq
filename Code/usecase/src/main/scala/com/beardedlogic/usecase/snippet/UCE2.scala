package com.beardedlogic.usecase
package snippet

import net.liftweb.http.{ StatefulSnippet, Templates }
import net.liftweb.http.SHtml
import net.liftweb.http.SHtml.ElemAttr.pairToBasic
import net.liftweb.http.js.{ JE, JsCmd, JsCmds }
import net.liftweb.http.js.JsCmds.jsExpToJsCmd
import net.liftweb.http.js.JsExp.strToJsExp
import net.liftweb.http.js.jquery.JqJE
import net.liftweb.util.ClearClearable
import net.liftweb.util.Helpers._
import scala.xml._

class UCE2 extends StatefulSnippet {
  import lib.field._

  override def dispatch = { case _ => render }

  val ucId = 1
  var title = "Untitled"
  val fields = Fields.DefaultFields.map(_.newFieldInstance)

  def render = {
    (
      ".ucdata *" #> renderFields(fields) andThen
      ".title .ucid *" #> ucId.toString
      & ".title @title [value]" #> title
    )
  }

  @inline def renderFields(fields: List[Field]) =
    fields.map(_.render).foldLeft(NodeSeq.Empty)(_ ++: _)
}
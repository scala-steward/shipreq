package com.beardedlogic.usecase
package snippet

import net.liftweb.http.SHtml
import net.liftweb.http.StatefulSnippet
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._
import net.liftweb.http.Templates
import scala.xml.NodeSeq
import net.liftweb.util.CssSel

/**
 * @since 29/04/13
 */
object UCEditor {

  case class UCStep(desc: String)

  val StepTemplate = Templates("_step_form" :: Nil).open_!

  val NewStep = UCStep("")
}

/**
 *
 * @since 26/04/2013
 */
class UCEditor extends StatefulSnippet {
  import UCEditor._

  private var steps = Vector(NewStep)

  override def dispatch = { case _ => render }

  def render =
    "#uce *" #> StepTemplate andThen
      "#uce form" #> steps.map(renderStep)

  private def renderStep(s: UCStep) = {
    var desc = ""
    "@desc" #> SHtml.textarea(s.desc, desc = _, "rows" -> "4") &
      "@add" #> SHtml.ajaxSubmit("Go!", () => addStep(desc))
  }

  private def addStep(desc: String): JsCmd = {
    steps = steps :+ new UCStep(desc)
    val newStepHtml: NodeSeq = renderStep(NewStep)(StepTemplate)
    Alert("Added " + desc) & AppendHtml("uce", newStepHtml)
  }
}

package com.beardedlogic.usecase
package snippet

import scala.collection.mutable.{ Map => MutableMap }
import net.liftweb.http.{ SHtml, StatefulSnippet, Templates }
import net.liftweb.http.js.{ JE, JsCmd, JsCmds }
import net.liftweb.util.ClearClearable
import net.liftweb.util.Helpers._
import lib.JsExt._
import net.liftweb.common._
import scala.annotation.tailrec
import scala.xml.Text
import net.liftweb.http.js.jquery.JqJsCmds
import net.liftweb.http.js.JsCmds.jsExpToJsCmd

/**
 * @since 29/04/13
 */
object UCEditor {

  case class Step(text: String)
  case class StepNode(id: String, level: Int, label: String, step: Step, children: List[StepNode])

  def NewStep = Step("")

  def StepTemplate = {
    val ExtractStepTemplate = ".step ^^" #> ""
    val index = Templates("index" :: Nil).open_!
    ExtractStepTemplate(ClearClearable(index))
  }

  /**
   * Flattens a list of step nodes with children, into a single list that contains all recursive contents.
   */
  @tailrec def flattenNodes(nodes: List[StepNode], results: List[StepNode] = Nil): List[StepNode] = nodes match {
    case Nil    => results
    case h :: t => flattenNodes(h.children ::: t, results :+ h)
  }

  /*
  case class StepNodeAndParent(node: StepNode, parent: Option[StepNode])

  def findNode(id: String)(nodes: List[StepNode], parent: Option[StepNode]): Box[StepNodeAndParent] = nodes match {
    case h :: t if h.id == id => Full(StepNodeAndParent(h, parent))
    case h :: t               => findNode(id)(h.children, Some(h)) or findNode(id)(t, parent)
    case _                    => Empty
  }
*/
  /*
  def insertNode(after: Option[StepNode], insert: StepNode, rem: List[StepNode]): List[StepNode] =
    if (after.isEmpty)
      insert :: incrementPosition(rem)
    else
      insertNode(after.get, insert, rem, Nil)

  @tailrec private def insertNode(
    after: StepNode,
    insert: StepNode,
    nodes: List[StepNode],
    results: List[StepNode]): List[StepNode] = nodes match {

    case Nil                  => results
    case h :: t if h == after => results ::: h :: insert :: incrementPosition(t)
    case h :: t               => insertNode(after, insert, t, results :+ h)
  }
*/
  def incrementPosition(n: StepNode) = {
    // TODO pos hack
    val posHack = (n.label.toInt + 1).toString
    n.copy(label = posHack)
  }

  @tailrec def incrementPosition(nodes: List[StepNode], results: List[StepNode] = Nil): List[StepNode] = nodes match {
    case h :: t => incrementPosition(t, results :+ incrementPosition(h))
    case Nil    => results
  }

  def insertStep(
    step: Step,
    afterId: String,
    nodes: List[StepNode],
    results: List[StepNode] = Nil,
    resultNode: Option[StepNode] = None): Tuple2[List[StepNode], Option[StepNode]] = nodes match {

    case Nil => (results, resultNode)

    case h :: t if h.id == afterId && h.level == 0 =>
      val n = StepNode(nextFuncName, h.level + 1, "1", step, Nil)
      val c = n :: incrementPosition(h.children)
      (results ::: h.copy(children = c) :: t, Some(n))

    case h :: t if h.id == afterId =>
      val n = StepNode(nextFuncName, h.level, h.label, step, Nil)
      (results ::: h :: incrementPosition(n :: t), Some(n))

    case h :: t =>
      val (c, n) = insertStep(step, afterId, h.children, Nil, resultNode)
      insertStep(step, afterId, t, results :+ h.copy(children = c), n)
  }
}

/**
 *
 * @since 26/04/2013
 */
class UCEditor extends StatefulSnippet {
  import UCEditor._
  override def dispatch = { case _ => render }

  val id = 1
  var title = ""
  val nodes = MutableMap[String, StepNode]()

  var courses: List[StepNode] =
    StepNode(nextFuncName, 0, s"${id}.0", NewStep,
      StepNode(nextFuncName, 1, "1", NewStep, Nil) :: Nil
    ) :: Nil

  def render = (
    "#steps *" #> StepTemplate
    andThen ".step" #> renderSteps(courses)
    & "#uc_id_num" #> id
    & "@title" #> SHtml.ajaxText(title, onTitleChange(_))
  )

  private def renderStep(n: StepNode) = (
    ".step [id]" #> n.id
    & ".step [class+]" #> s"lvl-${n.level}"
    & ".posTarget" #> n.label
    & ".pos [id]" #> posId(n)
    & "@text" #> SHtml.textarea(n.step.text, (_) => (), "rows" -> "4", "id" -> stepTextId(n))
    & ".add *" #> SHtml.ajaxButton("Add", () => onAddStep(n.id))
  )

  private def renderSteps(nodes: List[StepNode]) =
    flattenNodes(nodes).map(renderStep)

  private def stepTextId(n: StepNode) = s"${n.id}-t"
  private def posId(n: StepNode) = s"${n.id}-p"

  /**
   * When the Use Case title is changed, this will update the Normal Course title unless the user has overridden it.
   */
  def onTitleChange(newTitle: String): JsCmd = {
    val oldTitle = title
    title = newTitle
    val ncId = stepTextId(courses.head)
    (
      JsCmds.JsIf(
        JE.JsEq(oldTitle, JE.ValById(ncId)),
        JsCmds.SetValById(ncId, newTitle))
    )
  }

  /**
   * Adds a new step, shuffling down subsequent steps and renumbering if necessary.
   */
  def onAddStep(preceedingNodeId: String): JsCmd = {

    val newStep = NewStep
    val (newCourses, newNode) = insertStep(newStep, preceedingNodeId, courses)
    if (newNode.isDefined) {
      courses = newCourses
      val n = newNode.get
      val fn = ".step" #> renderStep(n)
      (
        JqId(preceedingNodeId) ~> JqAfter(fn(StepTemplate))
        & JqId(n.id) ~> JqHide ~> JqSlideDownFast
        & (for (n <- flattenNodes(courses))
          yield JsCmds.SetHtml(posId(n), Text(n.label + "."))) // Plus "."??? HACK
      )
    } else
      JsCmds.Noop
  }
}

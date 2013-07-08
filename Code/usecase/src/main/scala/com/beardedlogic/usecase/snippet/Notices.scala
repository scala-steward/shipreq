package com.beardedlogic.usecase.snippet

import net.liftweb.http.S
import net.liftweb.util.Helpers._
import scala.xml._
import net.liftweb.common.Box

/**
 * Renders any notices set in S.error/warning/notice.
 */
object Notices {

  final val ErrorClasses = "alert alert-error"
  final val WarningClasses = "alert"
  final val SuccessClasses = "alert alert-success"

  def render =
    "* *" #> (
      renderMsgsWithoutIds(ErrorClasses, S.errors) ++
      renderMsgsWithoutIds(WarningClasses, S.warnings) ++
      renderMsgsWithoutIds(SuccessClasses, S.notices)
    )

  def renderMsgsWithoutIds(classes: String, msgs: List[(NodeSeq, Box[String])]) =
    renderMsgs(classes, S.noIdMessages(msgs))

  def renderMsgs(classes: String, msgs: Seq[NodeSeq]): NodeSeq =
    msgs match {
      case Nil        => NodeSeq.Empty
      case one :: Nil => toMsgContainer(classes, one)
      case many       => toMsgContainer(classes + " alert-block", mergeMsgs(many))
    }

  def renderSingle(classes: String, msg: NodeSeq): Elem = toMsgContainer(classes, msg)

  def mergeMsgs(msgs: Seq[NodeSeq]): Elem = toList(msgs.map(toListItem).foldLeft(NodeSeq.Empty)(_ ++ _))

  // -------------------------------------------------------------------------------------------------------------------
  // HTML generation

  final val closeAlertButton              = <button type="button" class="close" data-dismiss="alert">&times;</button>
  private def toMsgContainer(
    classes: String, content: NodeSeq)    = <div class={classes}>{closeAlertButton}{content}</div>
  private def toList(msgs: NodeSeq)       = <ul>{msgs}</ul>
  private def toListItem(msg: NodeSeq)    = <li>{msg}</li>
}

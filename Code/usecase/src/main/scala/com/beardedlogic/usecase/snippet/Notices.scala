package com.beardedlogic.usecase.snippet

import net.liftweb.http.S
import net.liftweb.util.Helpers._
import scala.xml.NodeSeq
import scala._
import net.liftweb.common.Box

/**
 * Renders any notices set in S.error/warning/notice.
 */
object Notices {

  def render =
    "*" #> containContainers(
      renderMsgs("notice_e", S.errors) ++
      renderMsgs("notice_w", S.warnings) ++
      renderMsgs("notice_n", S.notices)
    )

  def renderMsgs(containerId: String, msgs: List[(NodeSeq, Box[String])]): NodeSeq = S.noIdMessages(msgs) match {
    case Nil => NodeSeq.Empty
    case one :: Nil => toMsgContainer(containerId, one)
    case many => toMsgContainer(containerId, mergeMsgs(many))
  }

  def mergeMsgs(msgs: List[NodeSeq]): NodeSeq = toList(msgs.map(toListItem).foldLeft(NodeSeq.Empty)(_ ++ _))

  def containContainers(containers: NodeSeq): NodeSeq =
    if (containers == NodeSeq.Empty) NodeSeq.Empty
    else toTopContainer(containers)

  def toTopContainer(containers: NodeSeq)          = <div id="notices">{containers}</div>
  def toMsgContainer(id: String, content: NodeSeq) = <div id={id}>{content}</div>
  def toList(msgs: NodeSeq)                        = <ul>{msgs}</ul>
  def toListItem(msg: NodeSeq)                     = <li>{msg}</li>
}

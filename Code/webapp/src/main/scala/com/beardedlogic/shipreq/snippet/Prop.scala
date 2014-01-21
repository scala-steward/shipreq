package com.beardedlogic.shipreq.snippet

import net.liftweb.http.DispatchSnippet
import net.liftweb.util.Helpers._
import scala.xml.NodeSeq
import com.beardedlogic.shipreq.app.AppConfig
import com.beardedlogic.shipreq.lib.Misc

object Prop extends DispatchSnippet {
  override def dispatch = { case s => renderMemo(s) }

  val renderMemo = Misc.newMemo[String, NodeSeq => NodeSeq]()(render)

  def render(n: String): NodeSeq => NodeSeq =
    n match {

      case "appName" =>
        "*" #> AppConfig.AppName

      case "supportEmailLink" =>
        "a [href]" #> ("mailto:" + AppConfig.SupportEmailAddress)
    }
}
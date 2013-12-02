package com.beardedlogic.shipreq.snippet

import net.liftweb.http.DispatchSnippet
import net.liftweb.util.Helpers._
import com.beardedlogic.shipreq.app.AppConfig

object Prop extends DispatchSnippet {

  override def dispatch = {
    case "appName" => appName
  }

  val appName = "*" #> AppConfig.AppName
}
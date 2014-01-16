package com.beardedlogic.shipreq.snippet

import com.beardedlogic.shipreq.app.AppConfig
import com.beardedlogic.shipreq.app.AppSiteMap
import com.beardedlogic.shipreq.app.AppSiteMap.Implicits._
import com.beardedlogic.shipreq.lib.{Misc, SnippetHelpers}
import net.liftweb.http.DispatchSnippet
import net.liftweb.sitemap.{Loc, SiteMap}
import scala.xml.{NodeSeq, Text}

/**
 * Creates a link to a page. Throws an error is the page is not found.
 */
object Link extends DispatchSnippet with SnippetHelpers {

  override def dispatch = {
    case "App" => appLink
    case name  => linkTo(name)
  }

  val appLink: NodeSeq => NodeSeq = {
    val v = <a href={AppSiteMap.Home.absoluteUrl}>{AppConfig.AppName}</a>
    _ => v
  }

  def linkTo(name: String): NodeSeq => NodeSeq = {
    val loc = SiteMap.findLoc(name) openOrThrowException s"Unable to generate link to $name"
    linkMemo(loc)
  }

  private val linkMemo =
    Misc.newMemo[Loc[_], NodeSeq => NodeSeq](Equiv.reference)(generateLink)

  def generateLink(loc: Loc[_]): NodeSeq => NodeSeq = {
      val linkText = loc.linkText openOr Text(loc.name)
      val link = <a href={loc.relativeUrl}>{linkText}</a>
      _ => link
    }
}
